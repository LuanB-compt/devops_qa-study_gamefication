package br.com.valueprojects.gamification.contribuicao.service;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import br.com.valueprojects.gamification.contribuicao.domain.Contribuicao;
import br.com.valueprojects.gamification.contribuicao.domain.MesDeContribuicoes;
import br.com.valueprojects.gamification.contribuicao.dto.ContribuicaoResponse;
import br.com.valueprojects.gamification.contribuicao.dto.RankingMesResponse;
import br.com.valueprojects.gamification.contribuicao.dto.VencedorResponse;
import br.com.valueprojects.gamification.contribuicao.entity.ContribuicaoEntity;
import br.com.valueprojects.gamification.contribuicao.entity.MesDeContribuicoesEntity;
import br.com.valueprojects.gamification.contribuicao.repository.ContribuicaoRepository;
import br.com.valueprojects.gamification.contribuicao.repository.MesDeContribuicoesRepository;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

/**
 * Orquestra a persistência de {@link MesDeContribuicoesEntity} e
 * {@link ContribuicaoEntity}, reaproveitando a lógica já testada de
 * {@link MesDeContribuicoes} (cenários 1, 3 e 4 do bdd.md) — a decisão de
 * ignorar contribuição duplicada, apurar o vencedor (com sorteio em caso de
 * empate) e conceder o curso são todas feitas pelo objeto de domínio, nunca
 * reimplementadas aqui.
 */
@Service
public class MesDeContribuicoesService {

    private final MesDeContribuicoesRepository mesRepository;
    private final ContribuicaoRepository contribuicaoRepository;
    private final AlunoService alunoService;

    public MesDeContribuicoesService(MesDeContribuicoesRepository mesRepository,
            ContribuicaoRepository contribuicaoRepository, AlunoService alunoService) {
        this.mesRepository = mesRepository;
        this.contribuicaoRepository = contribuicaoRepository;
        this.alunoService = alunoService;
    }

    public MesDeContribuicoesEntity buscarOuCriarMes(String descricao) {
        return mesRepository.findByDescricao(descricao)
                .orElseGet(() -> mesRepository.save(new MesDeContribuicoesEntity(descricao)));
    }

    /**
     * Cenário 4: reaproveita {@link MesDeContribuicoes#registrar(Contribuicao)}
     * para decidir se a nova contribuição deve ser ignorada (mesmo aluno da
     * última contribuição registrada no mês). Só persiste quando o domínio
     * de fato aceitou o registro.
     */
    @Transactional
    public boolean registrarContribuicao(String mesDescricao, String alunoNome, double peso) {
        MesDeContribuicoesEntity mesEntity = buscarOuCriarMes(mesDescricao);
        AlunoEntity alunoEntity = alunoService.buscarOuCriar(alunoNome);

        MesDeContribuicoes mesDominio = carregarDominio(mesEntity);
        int tamanhoAntes = mesDominio.getContribuicoes().size();

        mesDominio.registrar(new Contribuicao(new Aluno(alunoNome), peso));
        boolean registrada = mesDominio.getContribuicoes().size() > tamanhoAntes;

        if (registrada) {
            contribuicaoRepository.save(new ContribuicaoEntity(alunoEntity, mesEntity, peso));
        }
        return registrada;
    }

    public RankingMesResponse consultarRanking(String mesDescricao) {
        MesDeContribuicoesEntity mesEntity = buscarMesOuFalhar(mesDescricao);
        List<ContribuicaoResponse> itens = contribuicaoRepository.findByMesOrderByIdAsc(mesEntity).stream()
                .map(c -> new ContribuicaoResponse(c.getAluno().getNome(), c.getPeso()))
                .toList();
        return new RankingMesResponse(mesDescricao, itens);
    }

    /**
     * Cenários 1 e 3: reaproveita {@link MesDeContribuicoes#encerrar()} para
     * apurar o vencedor (com sorteio em caso de empate) e conceder o curso a
     * ele. O incremento de cursosConcluidos é o delta calculado pelo próprio
     * domínio (via {@link Aluno#ganharCurso()}), aplicado sobre o estado já
     * persistido do aluno.
     */
    @Transactional
    public VencedorResponse encerrarMes(String mesDescricao) {
        MesDeContribuicoesEntity mesEntity = buscarMesOuFalhar(mesDescricao);
        MesDeContribuicoes mesDominio = carregarDominio(mesEntity);

        Aluno vencedorDominio = mesDominio.encerrar();
        if (vencedorDominio == null) {
            return new VencedorResponse(mesDescricao, null, 0);
        }

        AlunoEntity vencedorEntity = alunoService.buscarPorNome(vencedorDominio.getNome());
        vencedorEntity.setCursosConcluidos(vencedorEntity.getCursosConcluidos() + vencedorDominio.getCursosConcluidos());
        return new VencedorResponse(mesDescricao, vencedorEntity.getNome(), vencedorEntity.getCursosConcluidos());
    }

    /**
     * Reconstrói o {@link MesDeContribuicoes} do domínio a partir das
     * contribuições já persistidas, na mesma ordem em que foram registradas.
     */
    private MesDeContribuicoes carregarDominio(MesDeContribuicoesEntity mesEntity) {
        MesDeContribuicoes mesDominio = new MesDeContribuicoes(mesEntity.getDescricao());
        for (ContribuicaoEntity contribuicaoEntity : contribuicaoRepository.findByMesOrderByIdAsc(mesEntity)) {
            Aluno alunoDominio = new Aluno(contribuicaoEntity.getAluno().getNome());
            mesDominio.registrar(new Contribuicao(alunoDominio, contribuicaoEntity.getPeso()));
        }
        return mesDominio;
    }

    private MesDeContribuicoesEntity buscarMesOuFalhar(String descricao) {
        return mesRepository.findByDescricao(descricao)
                .orElseThrow(() -> new EntityNotFoundException("Mês de contribuições não encontrado: " + descricao));
    }
}
