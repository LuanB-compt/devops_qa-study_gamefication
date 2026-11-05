package br.com.valueprojects.gamification.contribuicao.domain;

import br.com.valueprojects.gamification.aluno.domain.Aluno;

/** Uma linha do ranking: o aluno e o total de pontos (soma dos pesos) no mês. */
public record PosicaoRanking(Aluno aluno, double pontos) {
}
