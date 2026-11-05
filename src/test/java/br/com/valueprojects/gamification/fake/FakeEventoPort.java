package br.com.valueprojects.gamification.fake;

import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;
import br.com.valueprojects.gamification.evento.service.EventoPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbox em memória. Guarda e devolve CÓPIAS, como o banco: mudar o objeto devolvido
 * não altera o "persistido" até chamar {@link #salvar}.
 */
public class FakeEventoPort implements EventoPort {

    private final Map<UUID, EventoDeDominio> persistidos = new LinkedHashMap<>();

    @Override
    public void salvar(EventoDeDominio evento) {
        persistidos.put(evento.getId(), copia(evento));
    }

    @Override
    public Optional<EventoDeDominio> buscarParaProcessar(UUID id) {
        return Optional.ofNullable(persistidos.get(id)).map(FakeEventoPort::copia);
    }

    @Override
    public List<UUID> idsProntosParaEntrega(Instant agora, int limite) {
        return persistidos.values().stream()
                .filter(e -> e.estaProntoParaEntrega(agora))
                .sorted(Comparator.comparing(EventoDeDominio::getCriadoEm))
                .limit(limite)
                .map(EventoDeDominio::getId)
                .toList();
    }

    @Override
    public long contarPorStatus(EventoDeDominio.Status status) {
        return persistidos.values().stream().filter(e -> e.getStatus() == status).count();
    }

    public List<EventoDeDominio> todos() {
        return new ArrayList<>(persistidos.values().stream().map(FakeEventoPort::copia).toList());
    }

    public EventoDeDominio unico() {
        if (persistidos.size() != 1) {
            throw new AssertionError("esperava 1 evento, havia " + persistidos.size());
        }
        return copia(persistidos.values().iterator().next());
    }

    private static EventoDeDominio copia(EventoDeDominio e) {
        return new EventoDeDominio(e.getId(), e.getTipo(), e.getPayload(), e.getCriadoEm(), e.getStatus(),
                e.getTentativas(), e.getProximaTentativaEm(), e.getUltimoErro());
    }
}
