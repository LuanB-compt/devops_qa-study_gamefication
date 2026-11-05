package br.com.valueprojects.gamification.contribuicao.service;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.service.AlunoPort;
import br.com.valueprojects.gamification.contribuicao.domain.Contribuicao;
import br.com.valueprojects.gamification.contribuicao.domain.MesDeContribuicoes;
import br.com.valueprojects.gamification.contribuicao.dto.ContribuicaoResponse;
import br.com.valueprojects.gamification.contribuicao.dto.RankingMesResponse;
import br.com.valueprojects.gamification.contribuicao.dto.VencedorResponse;
import br.com.valueprojects.gamification.shared.RecursoNaoEncontradoException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service (casos de uso) do mês de contribuições. NÃO contém regra
 * de negócio: busca o agregado pela porta, chama o método de negócio do domínio
 * e manda salvar. Ranking, desempate, imutabilidade do mês encerrado e
 * idempotência da apuração vivem em {@link MesDeContribuicoes}.
 *
 * <p>Também não conhece JPA: depende só de portas ({@link MesDeContribuicoesPort},
 * {@link AlunoPort}). O ArchUnit reprova o build se essa fronteira for violada.
 */
@Service
public class MesDeContribuicoesService {

    private final MesDeContribuicoesPort mesPort;
    private final AlunoPort alunoPort;

    public MesDeContribuicoesService(MesDeContribuicoesPort mesPort, AlunoPort alunoPort) {
        this.mesPort = mesPort;
        this.alunoPort = alunoPort;
    }

    public MesDeContribuicoes buscarOuCriarMes(String descricao) {
        return mesPort.buscarOuCriar(descricao);
    }

    /** Cenário 4. @return {@code false} se o domínio recusou (consecutiva do mesmo aluno ou mês encerrado). */
    @Transactional
    public boolean registrarContribuicao(String mesDescricao, String alunoNome, double peso) {
        MesDeContribuicoes mes = mesPort.buscarOuCriar(mesDescricao);
        if (mes.isEncerrado()) {
            // Evita criar aluno por uma contribuição que o domínio vai recusar de qualquer forma.
            return false;
        }
        Aluno aluno = alunoPort.obterOuCriar(alunoNome);
        boolean registrada = mes.registrar(new Contribuicao(aluno, peso));
        if (registrada) {
            mesPort.salvar(mes);
        }
        return registrada;
    }

    /** Ranking = soma dos pesos por aluno, do maior para o menor. */
    public RankingMesResponse consultarRanking(String mesDescricao) {
        MesDeContribuicoes mes = mesPort.buscarPorDescricao(mesDescricao)
                .orElseThrow(() -> mesNaoEncontrado(mesDescricao));
        List<ContribuicaoResponse> itens = mes.ranking().stream()
                .map(p -> new ContribuicaoResponse(p.aluno().getNome(), p.pontos()))
                .toList();
        return new RankingMesResponse(mesDescricao, itens);
    }

    /** Cenários 1 e 3. Idempotente: a regra está no domínio; aqui só se evita regravar. */
    @Transactional
    public VencedorResponse encerrarMes(String mesDescricao) {
        MesDeContribuicoes mes = mesPort.buscarParaEncerramento(mesDescricao)
                .orElseThrow(() -> mesNaoEncontrado(mesDescricao));
        boolean jaEncerrado = mes.isEncerrado();
        Aluno vencedor = mes.encerrar();
        if (!jaEncerrado) {
            mesPort.salvar(mes);
        }
        return vencedor == null
                ? new VencedorResponse(mes.getDescricao(), null, 0)
                : new VencedorResponse(mes.getDescricao(), vencedor.getNome(), vencedor.getCursosConcluidos());
    }

    private RecursoNaoEncontradoException mesNaoEncontrado(String descricao) {
        return new RecursoNaoEncontradoException("Mês de contribuições não encontrado: " + descricao);
    }
}
