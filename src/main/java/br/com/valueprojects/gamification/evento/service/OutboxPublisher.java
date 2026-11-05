package br.com.valueprojects.gamification.evento.service;

import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava o evento na tabela outbox, dentro da transação do fato de negócio.
 *
 * <p>{@code MANDATORY} é a decisão central do ASR-6 em uma linha: se alguém tentar
 * publicar sem transação, falha de forma ruidosa em vez de gravar o evento em uma
 * transação própria (que poderia confirmar enquanto o fato dá rollback, ou o inverso).
 */
@Service
public class OutboxPublisher implements EventoPublisherPort {

    private final EventoPort eventoPort;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxPublisher(EventoPort eventoPort, ObjectMapper objectMapper, Clock clock) {
        this.eventoPort = eventoPort;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publicar(String tipo, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // Erro de programação do produtor: falhar aqui desfaz o fato inteiro, que é o correto.
            throw new IllegalArgumentException("Payload do evento " + tipo + " não serializável", e);
        }
        EventoDeDominio evento = EventoDeDominio.novo(tipo, json, clock.instant());
        eventoPort.salvar(evento);
        return evento.getId();
    }
}
