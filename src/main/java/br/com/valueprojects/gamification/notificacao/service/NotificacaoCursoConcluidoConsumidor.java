package br.com.valueprojects.gamification.notificacao.service;

import br.com.valueprojects.gamification.curso.domain.CursoConcluido;
import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;
import br.com.valueprojects.gamification.evento.service.EventoConsumidorPort;
import br.com.valueprojects.gamification.notificacao.domain.EmailMensagem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Consumidor que transforma "curso concluído" em um e-mail ao aluno. Não tem estado nem
 * transação própria: a idempotência vem do id do evento, repassado ao provedor.
 */
@Component
public class NotificacaoCursoConcluidoConsumidor implements EventoConsumidorPort {

    private final EmailPort emailPort;
    private final ObjectMapper objectMapper;

    public NotificacaoCursoConcluidoConsumidor(EmailPort emailPort, ObjectMapper objectMapper) {
        this.emailPort = emailPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public String tipo() {
        return CursoConcluido.TIPO;
    }

    @Override
    public void tratar(EventoDeDominio evento) throws Exception {
        CursoConcluido concluido = objectMapper.readValue(evento.getPayload(), CursoConcluido.class);
        emailPort.enviar(EmailMensagem.paraCursoConcluido(evento.getId(), concluido));
    }
}
