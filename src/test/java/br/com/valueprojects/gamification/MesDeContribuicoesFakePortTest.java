package br.com.valueprojects.gamification;

import br.com.valueprojects.gamification.aluno.service.AlunoPort;
import br.com.valueprojects.gamification.contribuicao.service.MesDeContribuicoesPort;
import br.com.valueprojects.gamification.fake.FakeAlunoPort;
import br.com.valueprojects.gamification.fake.FakeMesDeContribuicoesPort;

/** Contrato executado contra o adapter EM MEMÓRIA. */
public class MesDeContribuicoesFakePortTest extends MesDeContribuicoesPortContract {

    private final FakeAlunoPort alunos = new FakeAlunoPort();
    private final MesDeContribuicoesPort porta = new FakeMesDeContribuicoesPort(alunos, limite -> 0);

    @Override
    protected MesDeContribuicoesPort porta() {
        return porta;
    }

    @Override
    protected AlunoPort alunos() {
        return alunos;
    }
}
