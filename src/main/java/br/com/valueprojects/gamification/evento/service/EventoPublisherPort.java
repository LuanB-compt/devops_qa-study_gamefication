package br.com.valueprojects.gamification.evento.service;

import java.util.UUID;

/**
 * Porta usada pelos módulos PRODUTORES (ex.: {@code curso}) para registrar um fato a
 * comunicar. O produtor não sabe quem consome, nem como a entrega acontece.
 */
public interface EventoPublisherPort {

    /**
     * Registra o evento na transação do chamador.
     *
     * @param payload objeto serializável em JSON (contrato publicado pelo produtor)
     * @return id do evento, usado como chave de idempotência pelo consumidor
     * @throws org.springframework.transaction.IllegalTransactionStateException se chamado
     *         sem transação ativa: publicar fora da transação do fato reabriria o risco de
     *         evento fantasma (fato desfeito, evento mantido) ou de evento perdido
     */
    UUID publicar(String tipo, Object payload);
}
