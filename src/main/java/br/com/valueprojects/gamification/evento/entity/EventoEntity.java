package br.com.valueprojects.gamification.evento.entity;

import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Linha da tabela {@code evento_outbox}. O id é gerado pela aplicação (UUID), e não pelo
 * banco, para que exista antes do insert e já possa servir de chave de idempotência.
 *
 * <p>O índice cobre a consulta do dispatcher (pendentes cuja hora de tentar já chegou).
 */
@Entity
@Table(name = "evento_outbox", indexes = @Index(name = "idx_evento_outbox_status_proxima",
        columnList = "status, proximaTentativaEm"))
public class EventoEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Column(nullable = false)
    private Instant criadoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventoDeDominio.Status status;

    @Column(nullable = false)
    private int tentativas;

    @Column(nullable = false)
    private Instant proximaTentativaEm;

    @Column(length = 500)
    private String ultimoErro;

    protected EventoEntity() {
        // exigido pelo JPA
    }

    public static EventoEntity de(EventoDeDominio evento) {
        EventoEntity entity = new EventoEntity();
        entity.id = evento.getId();
        entity.tipo = evento.getTipo();
        entity.payload = evento.getPayload();
        entity.criadoEm = evento.getCriadoEm();
        entity.atualizarDe(evento);
        return entity;
    }

    /** Só o estado de entrega muda depois da criação; tipo e payload são imutáveis. */
    public void atualizarDe(EventoDeDominio evento) {
        this.status = evento.getStatus();
        this.tentativas = evento.getTentativas();
        this.proximaTentativaEm = evento.getProximaTentativaEm();
        this.ultimoErro = evento.getUltimoErro();
    }

    public EventoDeDominio paraDominio() {
        return new EventoDeDominio(id, tipo, payload, criadoEm, status, tentativas, proximaTentativaEm, ultimoErro);
    }

    public UUID getId() {
        return id;
    }
}
