package br.com.valueprojects.gamification.contribuicao.domain;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
/**
 * Representa a contribuição de um {@link Aluno} no fórum durante um
 * {@link MesDeContribuicoes} (equivalente ao "Resultado" do domínio de referência).
 * Atende os cenários 1, 3 e 4 do bdd.md, que dependem do registro e da
 * comparação de contribuições entre alunos.
 */
public class Contribuicao {

    private Aluno aluno;
    private double peso;

    public Contribuicao(Aluno aluno, double peso) {
        this.aluno = aluno;
        this.peso = peso;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public double getPeso() {
        return peso;
    }
}
