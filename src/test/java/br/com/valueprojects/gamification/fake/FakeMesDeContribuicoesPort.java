package br.com.valueprojects.gamification.fake;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.contribuicao.domain.Contribuicao;
import br.com.valueprojects.gamification.contribuicao.domain.MesDeContribuicoes;
import br.com.valueprojects.gamification.contribuicao.domain.Sorteador;
import br.com.valueprojects.gamification.contribuicao.service.MesDeContribuicoesPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter em memória da {@link MesDeContribuicoesPort}: o "trocar o adapter"
 * do mini-lab. Os casos de uso rodam sem Spring, sem banco e com o sorteio
 * controlado pelo teste. A MESMA suíte de contrato (MesDeContribuicoesPortContract)
 * valida este fake e o adapter JPA.
 *
 * <p>Limite assumido: não simula o lock pessimista (SELECT FOR UPDATE).
 */
public class FakeMesDeContribuicoesPort implements MesDeContribuicoesPort {

    private record Registro(String alunoNome, double peso) { }

    private static final class Estado {
        final List<Registro> contribuicoes = new ArrayList<>();
        boolean encerrado;
        String vencedorNome;
    }

    private final Map<String, Estado> meses = new HashMap<>();
    private final FakeAlunoPort alunos;
    private final Sorteador sorteador;

    public FakeMesDeContribuicoesPort(FakeAlunoPort alunos, Sorteador sorteador) {
        this.alunos = alunos;
        this.sorteador = sorteador;
    }

    @Override
    public Optional<MesDeContribuicoes> buscarPorDescricao(String descricao) {
        return Optional.ofNullable(meses.get(descricao)).map(e -> paraDominio(descricao, e));
    }

    @Override
    public Optional<MesDeContribuicoes> buscarParaEncerramento(String descricao) {
        return buscarPorDescricao(descricao);
    }

    @Override
    public MesDeContribuicoes buscarOuCriar(String descricao) {
        return paraDominio(descricao, meses.computeIfAbsent(descricao, d -> new Estado()));
    }

    @Override
    public void salvar(MesDeContribuicoes mes) {
        Estado estado = meses.computeIfAbsent(mes.getDescricao(), d -> new Estado());
        estado.contribuicoes.clear();
        for (Contribuicao c : mes.getContribuicoes()) {
            alunos.obterOuCriar(c.getAluno().getNome());
            estado.contribuicoes.add(new Registro(c.getAluno().getNome(), c.getPeso()));
        }
        estado.encerrado = mes.isEncerrado();
        if (mes.getVencedor() != null) {
            alunos.salvar(mes.getVencedor());
            estado.vencedorNome = mes.getVencedor().getNome();
        }
    }

    private MesDeContribuicoes paraDominio(String descricao, Estado estado) {
        Map<String, Aluno> cache = new HashMap<>();
        List<Contribuicao> contribuicoes = estado.contribuicoes.stream()
                .map(r -> new Contribuicao(cache.computeIfAbsent(r.alunoNome(), alunos::obterOuCriar), r.peso()))
                .toList();
        Aluno vencedor = estado.vencedorNome == null
                ? null
                : cache.computeIfAbsent(estado.vencedorNome, alunos::obter);
        return MesDeContribuicoes.restaurar(descricao, contribuicoes, estado.encerrado, vencedor, sorteador);
    }
}
