package br.com.valueprojects.gamification.evento.repository;

import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;
import br.com.valueprojects.gamification.evento.entity.EventoEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoRepository extends JpaRepository<EventoEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EventoEntity e where e.id = :id")
    Optional<EventoEntity> buscarComLock(@Param("id") UUID id);

    @Query("select e.id from EventoEntity e where e.status = :status and e.proximaTentativaEm <= :agora "
            + "order by e.criadoEm asc")
    List<UUID> idsProntos(@Param("status") EventoDeDominio.Status status, @Param("agora") Instant agora,
            Pageable limite);

    long countByStatus(EventoDeDominio.Status status);
}
