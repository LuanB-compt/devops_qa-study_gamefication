package br.com.valueprojects.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.valueprojects.gamification.curso.domain.CursoConcluido;
import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;
import br.com.valueprojects.gamification.evento.domain.EventoDeDominio.Status;
import br.com.valueprojects.gamification.evento.service.EventoConsumidorPort;
import br.com.valueprojects.gamification.evento.service.OutboxDispatcher;
import br.com.valueprojects.gamification.evento.service.ProcessadorDeEvento;
import br.com.valueprojects.gamification.evento.service.ProcessadorDeEvento.Resultado;
import br.com.valueprojects.gamification.fake.FakeEmailPort;
import br.com.valueprojects.gamification.fake.FakeEventoPort;
import br.com.valueprojects.gamification.fake.RelogioControlavel;
import br.com.valueprojects.gamification.notificacao.service.NotificacaoCursoConcluidoConsumidor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Entrega da outbox SEM Spring e SEM banco: fakes em memória e relógio controlável.
// Aqui mora a simulação do "consumidor indisponível" do desafio, e a prova de que a
// falha do consumidor fica contida no evento (nada volta para a conclusão do curso).
public class OutboxDispatcherTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeEventoPort outbox;
    private FakeEmailPort email;
    private RelogioControlavel relogio;
    private SimpleMeterRegistry metricas;
    private ProcessadorDeEvento processador;
    private OutboxDispatcher dispatcher;

    @BeforeEach
    void montar() {
        outbox = new FakeEventoPort();
        email = new FakeEmailPort();
        relogio = new RelogioControlavel();
        metricas = new SimpleMeterRegistry();
        processador = new ProcessadorDeEvento(outbox,
                List.of(new NotificacaoCursoConcluidoConsumidor(email, mapper)), relogio);
        dispatcher = new OutboxDispatcher(outbox, processador, relogio, metricas, 50);
    }

    private EventoDeDominio publicar(String aluno) throws Exception {
        CursoConcluido payload = new CursoConcluido(aluno, "Docker", 9.0, 4, 4, false);
        EventoDeDominio evento = EventoDeDominio.novo(CursoConcluido.TIPO, mapper.writeValueAsString(payload),
                relogio.instant());
        outbox.salvar(evento);
        return evento;
    }

    @Test
    public void deveEntregarEventoPendenteEMarcarComoEnviado() throws Exception {
        EventoDeDominio evento = publicar("Ana");

        int entregues = dispatcher.despachar();

        assertEquals(1, entregues);
        assertEquals(Status.ENVIADO, outbox.unico().getStatus());
        assertEquals(1, email.enviados().size());
        assertEquals(evento.getId(), email.enviados().get(0).idempotencyKey());
    }

    // Núcleo do desafio: consumidor fora do ar => nada se perde, nada quebra, volta sozinho.
    @Test
    public void consumidorIndisponivelMantemEventoPendenteERetentaQuandoVolta() throws Exception {
        publicar("Ana");
        email.derrubar();

        // When: provedor fora do ar
        int entregues = dispatcher.despachar();

        // Then: evento preservado, com a falha registrada
        EventoDeDominio pendente = outbox.unico();
        assertEquals(0, entregues);
        assertEquals(Status.PENDENTE, pendente.getStatus());
        assertEquals(1, pendente.getTentativas());
        assertTrue(pendente.getUltimoErro().contains("indisponível"));

        // Backoff: antes da hora não insiste (não martela um provedor que já está mal)
        relogio.avancar(Duration.ofSeconds(10));
        dispatcher.despachar();
        assertEquals(1, email.tentativas());

        // When: provedor volta e o horário da retentativa chega
        email.religar();
        relogio.avancar(Duration.ofSeconds(30));
        dispatcher.despachar();

        // Then: entregue exatamente uma vez
        assertEquals(Status.ENVIADO, outbox.unico().getStatus());
        assertEquals(1, email.enviados().size());
    }

    @Test
    public void deveEntregarTodoOBacklogAcumuladoDuranteAIndisponibilidade() throws Exception {
        email.derrubar();
        publicar("Ana");
        publicar("Bruno");
        publicar("Carla");
        dispatcher.despachar();
        assertEquals(3, outbox.contarPorStatus(Status.PENDENTE));

        email.religar();
        relogio.avancar(Duration.ofMinutes(1));
        int entregues = dispatcher.despachar();

        assertEquals(3, entregues);
        assertEquals(3, email.enviados().size());
        assertEquals(0, outbox.contarPorStatus(Status.PENDENTE));
    }

    @Test
    public void falhaDeUmEventoNaoBloqueiaOsDemais() throws Exception {
        publicar("Ana");
        publicar("Bruno");
        email.derrubarPara("ana");

        int entregues = dispatcher.despachar();

        assertEquals(1, entregues);
        assertEquals(1, outbox.contarPorStatus(Status.ENVIADO));
        assertEquals(1, outbox.contarPorStatus(Status.PENDENTE));
    }

    @Test
    public void aoEsgotarTentativasEventoViraFalhouENaoEhMaisRetentado() throws Exception {
        publicar("Ana");
        email.derrubar();

        for (int i = 0; i < EventoDeDominio.MAX_TENTATIVAS; i++) {
            dispatcher.despachar();
            relogio.avancar(Duration.ofHours(1)); // sempre além do backoff
        }
        int tentativasAteAqui = email.tentativas();
        dispatcher.despachar();

        assertEquals(Status.FALHOU, outbox.unico().getStatus());
        assertEquals(EventoDeDominio.MAX_TENTATIVAS, tentativasAteAqui);
        assertEquals(tentativasAteAqui, email.tentativas());
        assertEquals(1.0, metricas.get("outbox.eventos.falhos").gauge().value());
    }

    // "Reentrega não duplica": o mesmo evento processado de novo (outra instância, varredura
    // sobreposta) não gera segundo e-mail, porque o estado ENVIADO já foi confirmado.
    @Test
    public void reentregaDeEventoJaEnviadoNaoDuplicaOEmail() throws Exception {
        EventoDeDominio evento = publicar("Ana");

        Resultado primeira = processador.processar(evento.getId());
        Resultado segunda = processador.processar(evento.getId());

        assertEquals(Resultado.ENVIADO, primeira);
        assertEquals(Resultado.IGNORADO, segunda);
        assertEquals(1, email.enviados().size());
    }

    @Test
    public void eventoInexistenteEhIgnorado() {
        assertEquals(Resultado.IGNORADO, processador.processar(UUID.randomUUID()));
    }

    @Test
    public void tipoSemConsumidorFalhaSemPerderOEvento() {
        EventoDeDominio orfao = EventoDeDominio.novo("TIPO_DESCONHECIDO", "{}", relogio.instant());
        outbox.salvar(orfao);

        Resultado resultado = processador.processar(orfao.getId());

        assertEquals(Resultado.REAGENDADO, resultado);
        assertTrue(outbox.unico().getUltimoErro().contains("Nenhum consumidor"));
    }

    @Test
    public void payloadIlegivelViraFalhaEmVezDeExcecaoNaVarredura() {
        EventoDeDominio quebrado = EventoDeDominio.novo(CursoConcluido.TIPO, "isto nao e json", relogio.instant());
        outbox.salvar(quebrado);

        int entregues = dispatcher.despachar();

        assertEquals(0, entregues);
        assertEquals(1, outbox.unico().getTentativas());
    }

    @Test
    public void doisConsumidoresParaOMesmoTipoSaoRejeitadosNaInicializacao() {
        EventoConsumidorPort a = new NotificacaoCursoConcluidoConsumidor(email, mapper);
        EventoConsumidorPort b = new NotificacaoCursoConcluidoConsumidor(email, mapper);

        assertThrows(IllegalStateException.class,
                () -> new ProcessadorDeEvento(outbox, List.of(a, b), relogio));
    }

    @Test
    public void metricasExpoemBacklogEResultadoDasEntregas() throws Exception {
        publicar("Ana");
        email.derrubar();
        dispatcher.despachar();

        assertEquals(1.0, metricas.get("outbox.eventos.pendentes").gauge().value());
        assertEquals(1.0, metricas.get("outbox.eventos.processados").tag("resultado", "reagendado")
                .counter().count());

        email.religar();
        relogio.avancar(Duration.ofMinutes(1));
        dispatcher.despachar();

        assertEquals(0.0, metricas.get("outbox.eventos.pendentes").gauge().value());
        assertEquals(1.0, metricas.get("outbox.eventos.processados").tag("resultado", "enviado")
                .counter().count());
    }
}
