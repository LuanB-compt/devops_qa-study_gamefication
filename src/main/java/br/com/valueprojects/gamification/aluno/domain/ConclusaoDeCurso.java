package br.com.valueprojects.gamification.aluno.domain;

/**
 * Fato produzido pelo domínio ao concluir um curso: o que de fato aconteceu com o aluno.
 *
 * <p>Existe para que quem precisa comunicar o resultado (o evento de notificação) o
 * receba pronto do domínio, em vez de reconstruí-lo comparando "antes e depois" no
 * service, o que duplicaria a regra do bônus e da promoção a Premium.
 *
 * @param cursosGanhos  total creditado nesta conclusão (1, ou 4 com o bônus de média)
 * @param bonusDeMedia  se a média acima de 7 concedeu os +3 cursos
 * @param virouPremium  se esta conclusão levou o aluno ao plano Premium
 */
public record ConclusaoDeCurso(String cursoNome, double media, int cursosGanhos,
        boolean bonusDeMedia, boolean virouPremium) {
}
