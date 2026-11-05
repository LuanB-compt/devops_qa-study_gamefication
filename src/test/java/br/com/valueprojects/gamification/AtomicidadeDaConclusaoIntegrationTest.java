package br.com.valueprojects.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import br.com.valueprojects.gamification.aluno.repository.AlunoRepository;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import br.com.valueprojects.gamification.curso.repository.CursoJpaAdapter;
import br.com.valueprojects.gamification.curso.repository.CursoRepository;
import br.com.valueprojects.gamification.curso.service.CursoService;
import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;
import br.com.valueprojects.gamification.evento.domain.EventoDeDominio.Status;
import br.com.valueprojects.gamification.evento.entity.EventoEntity;
import br.com.valueprojects.gamification.evento.repository.EventoJpaAdapter;
import br.com.valueprojects.gamification.evento.repository.EventoRepository;
import br.com.valueprojects.gamification.evento.service.EventoPort;
import br.com.valueprojects.gamification.evento.service.EventoPublisherPort;
import br.com.valueprojects.gamification.evento.service.OutboxDispatcher;
import br.com.valueprojects.gamification.evento.service.OutboxPublisher;
import br.com.valueprojects.gamification.evento.service.ProcessadorDeEvento;
import br.com.valueprojects.gamification.fake.FakeEmailPort;
import br.com.valueprojects.gamification.fake.RelogioControlavel;
import br.com.valueprojects.gamification.notificacao.service.NotificacaoCursoConcluidoConsumidor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Evidência do ASR-6 (Consistência) com JPA + H2 reais.
 *
 * <p>O {@code @DataJpaTest} normalmente envolve cada teste em uma transação com rollback, o
 * que ESCONDERIA o que se quer provar: aqui o commit e o rollback são os do próprio
 * {@code CursoService}. Por isso a classe usa {@code NOT_SUPPORTED} e cada teste lê o banco
 * depois, pelos repositórios.
 *
 * <p>Os testes cobrem as três metades do cenário de qualidade: (1) commit grava recompensa
 * e evento juntos; (2) rollback não deixa evento fantasma nem recompensa órfã; (3) a falha
 * do consumidor não toca a recompensa, e a reentrega não a duplica.
 */
@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({ CursoService.class, CursoJpaAdapter.class, AlunoService.class, OutboxPublisher.class,
        EventoJpaAdapter.class, ProcessadorDeEvento.class, NotificacaoCursoConcluidoConsumidor.class,
        AtomicidadeDaConclusaoIntegrationTest.Infra.class })
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class AtomicidadeDaConclusaoIntegrationTest {

    @TestConfiguration
    static class Infra {
        @Bean
        FakeEmailPort emailFake() {
            return new FakeEmailPort();
        }

        // @Primary: o app define um Clock de produção; o teste precisa controlar o tempo.
        @Bean
        @Primary
        RelogioControlavel relogioFalso() {
            return new RelogioControlavel();
        }
    }

    @Autowired
    private CursoService cursoService;
    @Autowired
    private ProcessadorDeEvento processador;
    @Autowired
    private EventoPublisherPort publisher;
    @Autowired
    private EventoPort eventoPort;
    @Autowired
    private AlunoRepository alunoRepository;
    @Autowired
    private CursoRepository cursoRepository;
    @Autowired
    private EventoRepository eventoRepository;
    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private FakeEmailPort email;
    @Autowired
    private RelogioControlavel relogio;

    // Spy no adapter da outbox (não no publisher, que é proxy transacional): permite
    // forçar "o insert do evento falhou" sem alterar nenhum código de produção.
    @SpyBean
    private EventoJpaAdapter eventoAdapter;

    private OutboxDispatcher dispatcher;

    @BeforeEach
    void limpar() {
        eventoRepository.deleteAll();
        alunoRepository.deleteAll();
        cursoRepository.deleteAll();
        email.religar();
        email.enviados().clear();
        dispatcher = new OutboxDispatcher(eventoPort, processador, relogio, new SimpleMeterRegistry(), 50);
    }

    private int cursosDe(String nome) {
        return alunoRepository.findByNome(nome).map(AlunoEntity::getCursosConcluidos).orElse(0);
    }

    // (1) Commit => recompensa + exatamente UM evento, juntos.
    @Test
    public void commitGravaARecompensaEExatamenteUmEventoPendente() {
        AlunoResponse resposta = cursoService.concluirCurso("Ana", "Docker", 9.0);

        assertEquals(4, resposta.cursosConcluidos());
        assertEquals(4, cursosDe("Ana"));
        List<EventoEntity> eventos = eventoRepository.findAll();
        assertEquals(1, eventos.size());
        EventoDeDominio evento = eventos.get(0).paraDominio();
        assertEquals(Status.PENDENTE, evento.getStatus());
        assertEquals("CURSO_CONCLUIDO", evento.getTipo());
        assertTrue(evento.getPayload().contains("\"alunoNome\":\"Ana\""));
    }

    // (2a) Rollback DEPOIS de publicar (falha em etapa posterior da mesma transação):
    // nem a recompensa nem o evento sobrevivem. É o "evento fantasma" que a outbox impede.
    @Test
    public void rollbackPosteriorNaoDeixaEventoFantasmaNemRecompensa() {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            cursoService.concluirCurso("Bruno", "Git", 9.0);
            throw new IllegalStateException("falha em outra etapa da mesma transação");
        }));

        assertEquals(0, eventoRepository.count());
        assertEquals(0, cursosDe("Bruno"));
        assertTrue(alunoRepository.findByNome("Bruno").isEmpty());
    }

    // (2b) A outra direção: se registrar o evento falha, a recompensa também é desfeita.
    // Nunca existe "fato sem evento" (o evento perdido).
    @Test
    public void falhaAoGravarOEventoDesfazARecompensa() {
        doThrow(new IllegalStateException("insert da outbox falhou")).when(eventoAdapter).salvar(any());

        assertThrows(IllegalStateException.class, () -> cursoService.concluirCurso("Carla", "Java", 9.0));

        assertEquals(0, eventoRepository.count());
        assertEquals(0, cursosDe("Carla"));
    }

    // (2c) Publicar sem transação é proibido (MANDATORY): evita o evento gravado "por fora".
    @Test
    public void publicarForaDeTransacaoEhRejeitado() {
        assertThrows(IllegalTransactionStateException.class,
                () -> publisher.publicar("CURSO_CONCLUIDO", "{}"));

        assertEquals(0, eventoRepository.count());
    }

    // (3) Núcleo do desafio de ponta a ponta: consumidor fora do ar não afeta a conclusão,
    // o evento aguarda, e a entrega posterior nem duplica o e-mail nem toca a recompensa.
    @Test
    public void consumidorIndisponivelNaoAfetaConclusaoNemRecompensaEReentregaNaoDuplica() {
        email.derrubar();

        // When: o aluno conclui com o provedor de e-mail FORA DO AR
        AlunoResponse resposta = cursoService.concluirCurso("Duda", "Kubernetes", 9.0);
        dispatcher.despachar();

        // Then: conclusão e recompensa confirmadas, evento aguardando
        assertEquals(4, resposta.cursosConcluidos());
        assertEquals(4, cursosDe("Duda"));
        EventoDeDominio pendente = eventoRepository.findAll().get(0).paraDominio();
        assertEquals(Status.PENDENTE, pendente.getStatus());
        assertEquals(1, pendente.getTentativas());
        assertTrue(email.enviados().isEmpty());

        // When: o provedor volta e a retentativa vence o backoff
        email.religar();
        relogio.avancar(Duration.ofMinutes(1));
        dispatcher.despachar();

        // Then: exatamente um e-mail, com a chave de idempotência do evento
        assertEquals(1, email.enviados().size());
        assertEquals(pendente.getId(), email.enviados().get(0).idempotencyKey());
        assertEquals(Status.ENVIADO, eventoRepository.findAll().get(0).paraDominio().getStatus());

        // When: reentrega (varredura repetida e processamento direto do mesmo evento)
        dispatcher.despachar();
        processador.processar(pendente.getId());

        // Then: sem segundo e-mail, e a recompensa segue intacta (não é reaplicada)
        assertEquals(1, email.enviados().size());
        assertEquals(4, cursosDe("Duda"));
        assertEquals(1, eventoRepository.count());
    }

    // O adapter respeita o backoff e o status (a consulta que sustenta o dispatcher).
    @Test
    public void adapterSoListaPendentesCujoHorarioChegou() {
        email.derrubar();
        cursoService.concluirCurso("Edu", "Terraform", 5.0);
        dispatcher.despachar(); // falha => próxima tentativa em +30s

        assertTrue(eventoPort.idsProntosParaEntrega(relogio.instant(), 10).isEmpty());
        assertEquals(1, eventoPort.idsProntosParaEntrega(relogio.instant().plusSeconds(30), 10).size());
        assertEquals(1, eventoPort.contarPorStatus(Status.PENDENTE));
        assertEquals(0, eventoPort.contarPorStatus(Status.ENVIADO));
    }
}
