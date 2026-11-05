package br.com.valueprojects.gamification.evento.service;

import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;

/**
 * Porta implementada pelos módulos CONSUMIDORES (ex.: {@code notificacao}). O
 * dispatcher encontra o consumidor pelo {@link #tipo()}. Assim, produtor e consumidor
 * só se conhecem pelo contrato do evento, e um novo consumidor não altera o fluxo
 * crítico da conclusão.
 */
public interface EventoConsumidorPort {

    /** Tipo de evento que este consumidor trata (um consumidor por tipo). */
    String tipo();

    /**
     * Trata o evento. Qualquer exceção significa "não entregue": o evento continua
     * pendente e será retentado com backoff. Pode ser chamado mais de uma vez para o
     * mesmo evento (entrega pelo menos uma vez), então deve ser idempotente quanto
     * ao provedor externo, usando {@link EventoDeDominio#getId()} como chave.
     */
    void tratar(EventoDeDominio evento) throws Exception;
}
