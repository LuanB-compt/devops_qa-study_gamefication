package br.com.valueprojects.gamification.contribuicao.domain;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agregado do mês de contribuições (equivalente ao "Jogo" do domínio de
 * referência). Concentra TODAS as regras do mês, para que mudar uma regra
 * altere só esta classe (ASR-2):
 * <ul>
 *   <li>Cenário 4: ignora contribuição consecutiva do mesmo aluno;</li>
 *   <li>ranking = soma dos pesos por aluno, em ordem decrescente;</li>
 *   <li>Cenário 1: o mais pontuado ganha um curso ao encerrar o mês;</li>
 *   <li>Cenário 3: empate no topo é resolvido por sorteio;</li>
 *   <li>mês encerrado é imutável e só é apurado uma vez (idempotência).</li>
 * </ul>
 * É Java puro: sem Spring, JPA ou banco.
 */
public class MesDeContribuicoes {

    private final String descricao;
    private final List<Contribuicao> contribuicoes;
    private final Sorteador sorteador;
    private boolean encerrado;
    private Aluno vencedor;

    public MesDeContribuicoes(String descricao) {
        this(descricao, Sorteador.aleatorio());
    }

    public MesDeContribuicoes(String descricao, Sorteador sorteador) {
        this.descricao = descricao;
        this.contribuicoes = new ArrayList<>();
        this.sorteador = sorteador;
    }

    /**
     * Reconstrói um mês já persistido SEM reaplicar as regras de {@link #registrar}.
     * Reidratar via {@code registrar()} reavaliaria o histórico com a regra
     * vigente: se a regra de duplicidade mudasse, contribuições antigas já
     * gravadas seriam descartadas silenciosamente.
     */
    public static MesDeContribuicoes restaurar(String descricao, List<Contribuicao> contribuicoes,
            boolean encerrado, Aluno vencedor, Sorteador sorteador) {
        MesDeContribuicoes mes = new MesDeContribuicoes(descricao, sorteador);
        mes.contribuicoes.addAll(contribuicoes);
        mes.encerrado = encerrado;
        mes.vencedor = vencedor;
        return mes;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isEncerrado() {
        return encerrado;
    }

    /** Vencedor gravado na apuração; nulo se o mês está aberto ou fechou sem contribuições. */
    public Aluno getVencedor() {
        return vencedor;
    }

    public List<Contribuicao> getContribuicoes() {
        return Collections.unmodifiableList(contribuicoes);
    }

    /**
     * Cenário 4: o segundo resultado consecutivo do mesmo aluno é ignorado.
     * Mês encerrado não aceita nada (senão o vencedor gravado ficaria defasado).
     *
     * @return {@code true} se a contribuição foi aceita
     */
    public boolean registrar(Contribuicao contribuicao) {
        if (encerrado || repeteUltimoAluno(contribuicao)) {
            return false;
        }
        contribuicoes.add(contribuicao);
        return true;
    }

    private boolean repeteUltimoAluno(Contribuicao contribuicao) {
        return !contribuicoes.isEmpty()
                && contribuicoes.get(contribuicoes.size() - 1).getAluno().equals(contribuicao.getAluno());
    }

    /** Soma de pesos por aluno, em ordem decrescente de pontos (estável: empate mantém a ordem de chegada). */
    public List<PosicaoRanking> ranking() {
        return pontuacaoOrdenada().stream()
                .map(e -> new PosicaoRanking(e.getKey(), e.getValue().doubleValue()))
                .toList();
    }

    /**
     * Cenário 3: entre os alunos empatados no topo, o vencedor é sorteado.
     * Pontos são somados em {@link BigDecimal} para que 0,1 + 0,2 empate com 0,3.
     */
    public Aluno apurarVencedor() {
        List<Map.Entry<Aluno, BigDecimal>> ordenada = pontuacaoOrdenada();
        if (ordenada.isEmpty()) {
            return null;
        }
        BigDecimal maior = ordenada.get(0).getValue();
        List<Aluno> candidatos = ordenada.stream()
                .filter(e -> e.getValue().compareTo(maior) == 0)
                .map(Map.Entry::getKey)
                .toList();
        return candidatos.size() == 1 ? candidatos.get(0) : candidatos.get(sorteador.sortear(candidatos.size()));
    }

    /**
     * Cenário 1: apura e concede o curso ao vencedor. Idempotente: se o mês já
     * foi encerrado, devolve o vencedor gravado sem sortear nem conceder de novo.
     */
    public Aluno encerrar() {
        if (encerrado) {
            return vencedor;
        }
        Aluno apurado = apurarVencedor();
        if (apurado != null) {
            apurado.ganharCurso();
        }
        this.vencedor = apurado;
        this.encerrado = true;
        return apurado;
    }

    private List<Map.Entry<Aluno, BigDecimal>> pontuacaoOrdenada() {
        Map<Aluno, BigDecimal> pontos = new LinkedHashMap<>();
        for (Contribuicao c : contribuicoes) {
            pontos.merge(c.getAluno(), BigDecimal.valueOf(c.getPeso()), BigDecimal::add);
        }
        // sorted() em stream ordenado é estável: preserva a ordem de chegada nos empates.
        return pontos.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .toList();
    }
}
