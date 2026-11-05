package br.com.valueprojects.gamification.evento.service;

import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrega UM evento, em transação própria.
 *
 * <p>Fica em bean separado do {@link OutboxDispatcher} de propósito: com uma transação
 * por evento, a falha de um não desfaz nem bloqueia os demais, e a chamada passa pelo
 * proxy do Spring (auto-invocação ignoraria o {@code @Transactional}).
 */
@Service
public class ProcessadorDeEvento {

    public enum Resultado {
        ENVIADO,
        /** Falhou, mas ainda há tentativas: foi reagendado com backoff. */
        REAGENDADO,
        /** Esgotou as tentativas: precisa de intervenção humana. */
        FALHOU,
        /** Já tratado por outra execução/instância, ou ainda não está na hora. */
        IGNORADO
    }

    private static final Logger LOG = LoggerFactory.getLogger(ProcessadorDeEvento.class);

    private final EventoPort eventoPort;
    private final Map<String, EventoConsumidorPort> consumidoresPorTipo = new HashMap<>();
    private final Clock clock;

    public ProcessadorDeEvento(EventoPort eventoPort, List<EventoConsumidorPort> consumidores, Clock clock) {
        this.eventoPort = eventoPort;
        this.clock = clock;
        for (EventoConsumidorPort consumidor : consumidores) {
            if (consumidoresPorTipo.putIfAbsent(consumidor.tipo(), consumidor) != null) {
                throw new IllegalStateException("Mais de um consumidor para o evento " + consumidor.tipo());
            }
        }
    }

    @Transactional
    public Resultado processar(UUID id) {
        Instant agora = clock.instant();
        // Relê COM lock: entre listar os ids e chegar aqui, outra execução pode ter entregue.
        Optional<EventoDeDominio> encontrado = eventoPort.buscarParaProcessar(id);
        if (encontrado.isEmpty() || !encontrado.get().estaProntoParaEntrega(agora)) {
            return Resultado.IGNORADO;
        }
        EventoDeDominio evento = encontrado.get();

        try {
            EventoConsumidorPort consumidor = consumidoresPorTipo.get(evento.getTipo());
            if (consumidor == null) {
                throw new IllegalStateException("Nenhum consumidor para o tipo " + evento.getTipo());
            }
            consumidor.tratar(evento);
            evento.marcarEnviado();
        } catch (Exception e) {
            // A falha do consumidor é registrada NO evento e nunca propagada: a conclusão do
            // curso já foi confirmada em outra transação e não depende do que acontece aqui.
            evento.registrarFalha(e.getClass().getSimpleName() + ": " + e.getMessage(), agora);
            LOG.warn("Entrega do evento {} ({}) falhou, tentativa {}: {}",
                    evento.getId(), evento.getTipo(), evento.getTentativas(), e.toString());
        }
        eventoPort.salvar(evento);

        return switch (evento.getStatus()) {
            case ENVIADO -> Resultado.ENVIADO;
            case FALHOU -> Resultado.FALHOU;
            case PENDENTE -> Resultado.REAGENDADO;
        };
    }
}
