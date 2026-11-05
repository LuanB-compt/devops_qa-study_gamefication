package br.com.valueprojects.gamification.assinatura.domain;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
/**
 * Vínculo entre um {@link Aluno} e o plano de assinatura mensal.
 * Atende o cenário 2: "Dada uma mensalidade não paga e o aluno está
 * ativo, quando encerra o mês e não é realizado o pagamento, então a
 * assinatura do aluno é cancelada".
 */
public class Assinatura {

    private Aluno aluno;
    private boolean ativa;
    private boolean pagamentoRealizado;

    public Assinatura(Aluno aluno) {
        this(aluno, true, false);
    }

    /**
     * Construtor usado pela camada de persistência (service)
     * para reconstruir uma Assinatura a partir do estado já salvo em banco,
     * sem alterar as regras de negócio dos métodos públicos.
     */
    public Assinatura(Aluno aluno, boolean ativa, boolean pagamentoRealizado) {
        this.aluno = aluno;
        this.ativa = ativa;
        this.pagamentoRealizado = pagamentoRealizado;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public boolean isPagamentoRealizado() {
        return pagamentoRealizado;
    }

    public void registrarPagamento() {
        pagamentoRealizado = true;
    }

    /**
     * Cenário 2: encerra o mês da assinatura; se o pagamento não foi realizado
     * e a assinatura estava ativa, ela é cancelada.
     */
    public void encerrarMes() {
        if (ativa && !pagamentoRealizado) {
            ativa = false;
        }
        pagamentoRealizado = false;
    }
}
