package br.com.valueprojects.gamification.notificacao.adapter;

import br.com.valueprojects.gamification.notificacao.domain.EmailMensagem;
import br.com.valueprojects.gamification.notificacao.service.EmailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter padrão: "envia" escrevendo o e-mail no log. Basta para dev/homol e para a
 * demonstração (o log mostra a entrega e a chave de idempotência). Trocar por SMTP é
 * escrever outro {@link EmailPort}; nenhum outro componente muda.
 */
@Component
public class LogEmailAdapter implements EmailPort {

    private static final Logger LOG = LoggerFactory.getLogger(LogEmailAdapter.class);

    @Override
    public void enviar(EmailMensagem mensagem) {
        LOG.info("E-mail enviado [Message-ID={}] para={} assunto=\"{}\"",
                mensagem.idempotencyKey(), mensagem.para(), mensagem.assunto());
    }
}
