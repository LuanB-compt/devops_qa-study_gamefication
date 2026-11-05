package br.com.valueprojects.gamification.aluno.domain;

import br.com.valueprojects.gamification.aluno.vo.Curso;

/**
 * Representa o aluno da plataforma de educação continuada gamificada.
 * Atende, ao menos em parte, todos os 5 cenários:
 * - Cenário 1: aluno com mais contribuições ganha um curso ao encerrar o mês.
 * - Cenário 2: aluno vinculado a uma {@link br.com.valueprojects.gamification.assinatura.domain.Assinatura}
 *   que pode ser cancelada. (Referência por nome completo, sem import: um import só para
 *   Javadoc criaria um ciclo aluno -> assinatura -> aluno no código-fonte.)
 * - Cenário 3: aluno pode ser o escolhido do mês em caso de sorteio por empate.
 * - Cenário 4: aluno é o "participante" cujas contribuições duplicadas são ignoradas.
 * - Cenário 5: aluno que conclui um curso com média acima de 7 ganha mais 3 cursos.
 */
public class Aluno {

    public enum Plano {
        BASICO,
        PREMIUM
    }

    private String nome;
    private int cursosConcluidos;
    private int moedas;
    private Plano plano;

    private static final int CURSOS_PARA_PREMIUM = 12;
    private static final int MOEDAS_AO_VIRAR_PREMIUM = 3;

    public Aluno(String nome) {
        this(nome, 0, 0, Plano.BASICO);
    }

    /** Reidratação: reconstrói o aluno a partir do estado persistido, sem aplicar regras. */
    public Aluno(String nome, int cursosConcluidos, int moedas, Plano plano) {
        this.nome = nome;
        this.cursosConcluidos = cursosConcluidos;
        this.moedas = moedas;
        this.plano = plano;
    }

    public String getNome() {
        return nome;
    }

    public int getCursosConcluidos() {
        return cursosConcluidos;
    }

    public int getMoedas() {
        return moedas;
    }

    public Plano getPlano() {
        return plano;
    }

    /**
     * Cenário 1: "Dado o aluno com mais contribuições, quando encerra o mês,
     * então o Aluno ganha um curso".
     */
    public void ganharCurso() {
        cursosConcluidos++;
    }

    /**
     * Cenário 5: "Dado um aluno que finalizou um curso e está com a média acima
     * de 7, quando finalizar o curso, então esse aluno ganhará mais 3 cursos".
     */
    public void concluirCurso(Curso curso, double media) {
        cursosConcluidos++;
        if (media > 7) {
            cursosConcluidos += 3;
        }
        promoverSeAtingiuDozeCursos();
    }

    /**
     * Regra do case: ao acumular 12 cursos o aluno vira Premium e ganha 3 moedas.
     * Antes vivia em CursoService; no domínio, uma mudança de regra não toca o caso de uso.
     * Só é avaliada na conclusão de curso (comportamento preservado da AC1).
     */
    private void promoverSeAtingiuDozeCursos() {
        if (plano == Plano.BASICO && cursosConcluidos >= CURSOS_PARA_PREMIUM) {
            plano = Plano.PREMIUM;
            moedas += MOEDAS_AO_VIRAR_PREMIUM;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Aluno other = (Aluno) obj;
        return nome == null ? other.nome == null : nome.equals(other.nome);
    }

    @Override
    public int hashCode() {
        return nome == null ? 0 : nome.hashCode();
    }
}
