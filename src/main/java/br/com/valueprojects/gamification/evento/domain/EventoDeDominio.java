package br.com.valueprojects.gamification.evento.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Intenção de comunicar um fato de negócio, gravada na MESMA transação que o fato (ASR-6).
 *
 * <p>É a linha da tabela "outbox": em vez de chamar o consumidor (e-mail, analytics) de
 * dentro da transação, o Core só registra aqui que algo precisa ser comunicado. O commit
 * confirma fato + recompensa + intenção juntos; o rollback desfaz os três. A entrega
 * acontece depois, fora da transação, e pode falhar e ser repetida sem afetar o fato.
 *
 * <p>Semântica de entrega: <b>pelo menos uma vez</b>. O {@link #getId() id} é a chave de
 * idempotência que o consumidor repassa ao provedor para neutralizar reentregas.
 */
public class EventoDeDominio {

    public enum Status {
        PENDENTE,
        ENVIADO,
        /** Esgotou as tentativas: precisa de intervenção humana (dead letter). */
        FALHOU
    }

    /** Depois disto o evento deixa de ser retentado e vira alerta operacional. */
    public static final int MAX_TENTATIVAS = 5;

    /** Atraso após a 1ª falha; dobra a cada nova falha (30s, 60s, 120s, 240s). */
    public static final Duration ATRASO_BASE = Duration.ofSeconds(30);

    private static final int TAMANHO_MAXIMO_ERRO = 500;

    private final UUID id;
    private final String tipo;
    private final String payload;
    private final Instant criadoEm;
    private Status status;
    private int tentativas;
    private Instant proximaTentativaEm;
    private String ultimoErro;

    /** Evento novo: nasce pendente e elegível para entrega imediata. */
    public static EventoDeDominio novo(String tipo, String payload, Instant agora) {
        return new EventoDeDominio(UUID.randomUUID(), tipo, payload, agora, Status.PENDENTE, 0, agora, null);
    }

    /** Reidratação a partir do estado persistido, sem aplicar regras. */
    public EventoDeDominio(UUID id, String tipo, String payload, Instant criadoEm, Status status,
            int tentativas, Instant proximaTentativaEm, String ultimoErro) {
        this.id = id;
        this.tipo = tipo;
        this.payload = payload;
        this.criadoEm = criadoEm;
        this.status = status;
        this.tentativas = tentativas;
        this.proximaTentativaEm = proximaTentativaEm;
        this.ultimoErro = ultimoErro;
    }

    /** Só eventos pendentes cujo horário de tentativa chegou são entregues. */
    public boolean estaProntoParaEntrega(Instant agora) {
        return status == Status.PENDENTE && !agora.isBefore(proximaTentativaEm);
    }

    public void marcarEnviado() {
        exigirPendente();
        status = Status.ENVIADO;
        ultimoErro = null;
    }

    /**
     * Registra uma tentativa fracassada. Reagenda com backoff exponencial; ao atingir
     * {@link #MAX_TENTATIVAS} o evento vira {@link Status#FALHOU} e sai da fila.
     */
    public void registrarFalha(String erro, Instant agora) {
        exigirPendente();
        tentativas++;
        ultimoErro = erro == null ? "erro desconhecido" : truncar(erro);
        if (tentativas >= MAX_TENTATIVAS) {
            status = Status.FALHOU;
        } else {
            proximaTentativaEm = agora.plus(ATRASO_BASE.multipliedBy(1L << (tentativas - 1)));
        }
    }

    private void exigirPendente() {
        if (status != Status.PENDENTE) {
            throw new IllegalStateException("Evento " + id + " não está pendente: " + status);
        }
    }

    private static String truncar(String texto) {
        return texto.length() <= TAMANHO_MAXIMO_ERRO ? texto : texto.substring(0, TAMANHO_MAXIMO_ERRO);
    }

    public UUID getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Status getStatus() {
        return status;
    }

    public int getTentativas() {
        return tentativas;
    }

    public Instant getProximaTentativaEm() {
        return proximaTentativaEm;
    }

    public String getUltimoErro() {
        return ultimoErro;
    }
}
