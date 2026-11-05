package br.com.valueprojects.gamification.evento.repository;

import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;
import br.com.valueprojects.gamification.evento.entity.EventoEntity;
import br.com.valueprojects.gamification.evento.service.EventoPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Adapter JPA da {@link EventoPort}. Não abre transação própria: o insert do evento
 * precisa participar da transação do fato de negócio (ASR-6).
 */
@Component
public class EventoJpaAdapter implements EventoPort {

    private final EventoRepository repository;

    public EventoJpaAdapter(EventoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void salvar(EventoDeDominio evento) {
        EventoEntity entity = repository.findById(evento.getId())
                .map(existente -> {
                    existente.atualizarDe(evento);
                    return existente;
                })
                .orElseGet(() -> EventoEntity.de(evento));
        repository.save(entity);
    }

    @Override
    public Optional<EventoDeDominio> buscarParaProcessar(UUID id) {
        return repository.buscarComLock(id).map(EventoEntity::paraDominio);
    }

    @Override
    public List<UUID> idsProntosParaEntrega(Instant agora, int limite) {
        return repository.idsProntos(EventoDeDominio.Status.PENDENTE, agora, PageRequest.of(0, limite));
    }

    @Override
    public long contarPorStatus(EventoDeDominio.Status status) {
        return repository.countByStatus(status);
    }
}
