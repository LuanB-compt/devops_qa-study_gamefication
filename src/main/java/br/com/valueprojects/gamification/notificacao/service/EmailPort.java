package br.com.valueprojects.gamification.notificacao.service;

import br.com.valueprojects.gamification.notificacao.domain.EmailMensagem;

/**
 * Porta de saída do envio de e-mail. É a "dependência externa" do case: o adapter real
 * (SMTP, SES...) fica atrás dela, e os testes simulam sua indisponibilidade com um fake.
 */
public interface EmailPort {

    /**
     * @throws RuntimeException se o provedor estiver indisponível; o evento então fica
     *         pendente e é retentado (nunca afeta a conclusão do curso)
     */
    void enviar(EmailMensagem mensagem);
}
