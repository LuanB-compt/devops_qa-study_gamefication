package br.com.valueprojects.gamification.evento.service;

import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Varre a outbox periodicamente e entrega o que estiver pendente.
 *
 * <p>Por que polling no próprio Core, sem broker: o volume do case é de poucos eventos
 * por minuto, e a tabela outbox já dá durabilidade e reentrega. Um broker acrescentaria
 * um serviço a operar sem resolver o problema real (atomicidade com o banco, que exige
 * a outbox de qualquer forma). Quando houver consumidores fora do processo, só este
 * componente é trocado por um relay para o broker; produtores e a outbox seguem iguais.
 *
 * <p>Observabilidade: o gauge {@code outbox_eventos_pendentes} é o sinal de backlog (cresce
 * quando o consumidor cai) e {@code outbox_eventos_processados_total} separa entrega, retentativa e falha.
 */
@Component
public class OutboxDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final EventoPort eventoPort;
    private final ProcessadorDeEvento processador;
    private final Clock clock;
    private final int lote;
    private final Map<ProcessadorDeEvento.Resultado, Counter> contadores =
            new EnumMap<>(ProcessadorDeEvento.Resultado.class);

    public OutboxDispatcher(EventoPort eventoPort, ProcessadorDeEvento processador, Clock clock,
            MeterRegistry registry, @Value("${eventos.dispatcher.lote:50}") int lote) {
        this.eventoPort = eventoPort;
        this.processador = processador;
        this.clock = clock;
        this.lote = lote;

        Gauge.builder("outbox.eventos.pendentes",
                () -> eventoPort.contarPorStatus(EventoDeDominio.Status.PENDENTE))
                .description("Eventos aguardando entrega (backlog da outbox)")
                .register(registry);
        Gauge.builder("outbox.eventos.falhos",
                () -> eventoPort.contarPorStatus(EventoDeDominio.Status.FALHOU))
                .description("Eventos que esgotaram as tentativas (exigem intervenção)")
                .register(registry);
        for (ProcessadorDeEvento.Resultado resultado : ProcessadorDeEvento.Resultado.values()) {
            contadores.put(resultado, Counter.builder("outbox.eventos.processados")
                    .tag("resultado", resultado.name().toLowerCase())
                    .register(registry));
        }
    }

    /**
     * {@code fixedDelay} (e não fixedRate): a próxima varredura só começa depois que a
     * anterior terminou, então um consumidor lento não empilha execuções.
     */
    @Scheduled(fixedDelayString = "${eventos.dispatcher.intervalo-ms:5000}")
    public int despachar() {
        List<UUID> ids = eventoPort.idsProntosParaEntrega(clock.instant(), lote);
        int entregues = 0;
        for (UUID id : ids) {
            try {
                ProcessadorDeEvento.Resultado resultado = processador.processar(id);
                contadores.get(resultado).increment();
                if (resultado == ProcessadorDeEvento.Resultado.ENVIADO) {
                    entregues++;
                }
            } catch (RuntimeException e) {
                // Falha de infraestrutura (ex.: banco) ao processar UM evento não pode
                // derrubar a varredura: o evento continua pendente e volta na próxima.
                LOG.error("Falha ao processar o evento {}", id, e);
            }
        }
        return entregues;
    }
}
