package br.com.valueprojects.gamification.contribuicao.domain;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Acumula as {@link Contribuicao}s de um mês (equivalente ao "Jogo" do domínio
 * de referência). Atende os cenários:
 * - Cenário 1: apura quem tem mais contribuições e concede um curso ao vencedor.
 * - Cenário 3: em caso de empate, o aluno do mês é escolhido por sorteio.
 * - Cenário 4: ignora o segundo registro de contribuição do mesmo aluno na mesma rodada.
 */
public class MesDeContribuicoes {

    private String descricao;
    private List<Contribuicao> contribuicoes;

    public MesDeContribuicoes(String descricao) {
        this.descricao = descricao;
        this.contribuicoes = new ArrayList<>();
    }

    /**
     * Cenário 4: "Dado um jogo acontecendo e um Participante válido, quando notar
     * dois ou mais resultados deste participante, então o segundo resultado deve
     * ser ignorado".
     */
    public void registrar(Contribuicao contribuicao) {
        if (contribuicoes.isEmpty()
                || !contribuicoes.get(ultimaContribuicaoVista()).getAluno().equals(contribuicao.getAluno())) {
            contribuicoes.add(contribuicao);
        }
    }

    private int ultimaContribuicaoVista() {
        return contribuicoes.size() - 1;
    }

    public List<Contribuicao> getContribuicoes() {
        return Collections.unmodifiableList(contribuicoes);
    }

    /**
     * Cenário 3: "Dado um empate em contribuições de alunos, quando o mês encerra,
     * então o aluno escolhido do mês será sorteado".
     */
    public Aluno apurarVencedor() {
        if (contribuicoes.isEmpty()) {
            return null;
        }

        Map<Aluno, Integer> contagemPorAluno = new LinkedHashMap<>();
        for (Contribuicao contribuicao : contribuicoes) {
            contagemPorAluno.merge(contribuicao.getAluno(), 1, Integer::sum);
        }

        int maiorContagem = Collections.max(contagemPorAluno.values());
        List<Aluno> candidatos = new ArrayList<>();
        for (Map.Entry<Aluno, Integer> entrada : contagemPorAluno.entrySet()) {
            if (entrada.getValue() == maiorContagem) {
                candidatos.add(entrada.getKey());
            }
        }

        if (candidatos.size() == 1) {
            return candidatos.get(0);
        }
        return candidatos.get(new Random().nextInt(candidatos.size()));
    }

    /**
     * Cenário 1: "Dado o aluno com mais contribuições, quando encerra o mês,
     * então o Aluno ganha um curso". Apura o vencedor e concede o curso a ele.
     */
    public Aluno encerrar() {
        Aluno vencedor = apurarVencedor();
        if (vencedor != null) {
            vencedor.ganharCurso();
        }
        return vencedor;
    }
}
