package br.com.valueprojects.gamification.fake;

import br.com.valueprojects.gamification.notificacao.domain.EmailMensagem;
import br.com.valueprojects.gamification.notificacao.service.EmailPort;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Provedor de e-mail controlável: o teste o "derruba" e o "religa", que é a simulação
 * de consumidor indisponível pedida pelo desafio.
 */
public class FakeEmailPort implements EmailPort {

    private final List<EmailMensagem> enviados = new ArrayList<>();
    private Predicate<EmailMensagem> indisponivelPara = m -> false;
    private int tentativas;

    public void derrubar() {
        indisponivelPara = m -> true;
    }

    public void derrubarPara(String enderecoPrefixo) {
        indisponivelPara = m -> m.para().startsWith(enderecoPrefixo);
    }

    public void religar() {
        indisponivelPara = m -> false;
    }

    @Override
    public void enviar(EmailMensagem mensagem) {
        tentativas++;
        if (indisponivelPara.test(mensagem)) {
            throw new IllegalStateException("provedor de e-mail indisponível");
        }
        enviados.add(mensagem);
    }

    public List<EmailMensagem> enviados() {
        return enviados;
    }

    /** Quantas vezes o Core TENTOU enviar (inclui as que falharam). */
    public int tentativas() {
        return tentativas;
    }
}
