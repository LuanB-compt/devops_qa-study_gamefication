package br.com.valueprojects.gamification.curso.domain;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.domain.ConclusaoDeCurso;

/**
 * Evento de domínio publicado quando um aluno conclui um curso. Este record é o
 * CONTRATO do evento (publicado pelo módulo {@code curso}); seus campos viram o JSON da
 * outbox, então renomear ou remover um campo é uma mudança de contrato para os consumidores.
 *
 * @param cursosGanhos          créditos desta conclusão (1, ou 4 com o bônus de média)
 * @param cursosConcluidosTotal acumulado do aluno já com esta conclusão
 */
public record CursoConcluido(String alunoNome, String cursoNome, double media, int cursosGanhos,
        int cursosConcluidosTotal, boolean virouPremium) {

    public static final String TIPO = "CURSO_CONCLUIDO";

    /** Monta o evento a partir do que o domínio acabou de decidir, sem recalcular regra. */
    public static CursoConcluido de(Aluno aluno, ConclusaoDeCurso conclusao) {
        return new CursoConcluido(aluno.getNome(), conclusao.cursoNome(), conclusao.media(),
                conclusao.cursosGanhos(), aluno.getCursosConcluidos(), conclusao.virouPremium());
    }
}
