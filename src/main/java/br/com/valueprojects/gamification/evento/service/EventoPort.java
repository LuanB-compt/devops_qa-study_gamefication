package br.com.valueprojects.gamification.evento.service;

import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência da outbox, em termos de domínio (mesmo padrão do ADR-002). */
public interface EventoPort {

    /** Insere ou atualiza. Participa da transação em curso do chamador. */
    void salvar(EventoDeDominio evento);

    /**
     * Carrega o evento TRAVANDO a linha ({@code SELECT ... FOR UPDATE}): se duas
     * instâncias do Core tentarem entregar o mesmo evento, a segunda espera e então
     * vê o estado já atualizado, em vez de enviar de novo. O lock é detalhe do adapter.
     */
    Optional<EventoDeDominio> buscarParaProcessar(UUID id);

    /** Ids dos eventos pendentes cujo horário de tentativa já chegou, do mais antigo ao mais novo. */
    List<UUID> idsProntosParaEntrega(Instant agora, int limite);

    long contarPorStatus(EventoDeDominio.Status status);
}
