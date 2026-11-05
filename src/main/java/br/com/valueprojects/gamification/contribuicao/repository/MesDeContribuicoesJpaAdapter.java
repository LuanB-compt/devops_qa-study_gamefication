package br.com.valueprojects.gamification.contribuicao.repository;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import br.com.valueprojects.gamification.contribuicao.domain.Contribuicao;
import br.com.valueprojects.gamification.contribuicao.domain.MesDeContribuicoes;
import br.com.valueprojects.gamification.contribuicao.domain.Sorteador;
import br.com.valueprojects.gamification.contribuicao.entity.ContribuicaoEntity;
import br.com.valueprojects.gamification.contribuicao.entity.MesDeContribuicoesEntity;
import br.com.valueprojects.gamification.contribuicao.service.MesDeContribuicoesPort;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Adapter JPA da {@link MesDeContribuicoesPort}: único lugar do módulo que
 * traduz {@link MesDeContribuicoes} (domínio) <-> entities. Trocar a
 * persistência (ou usar um fake em teste) não toca nenhuma regra.
 *
 * <p>Usa {@link AlunoService} (e {@link AlunoEntity}) só para obter a chave
 * estrangeira do aluno: é a parcela da dívida técnica que sobra, agora
 * confinada a este adapter e às associações JPA.
 */
@Component
public class MesDeContribuicoesJpaAdapter implements MesDeContribuicoesPort {

    private final MesDeContribuicoesRepository mesRepository;
    private final ContribuicaoRepository contribuicaoRepository;
    private final AlunoService alunoService;

    public MesDeContribuicoesJpaAdapter(MesDeContribuicoesRepository mesRepository,
            ContribuicaoRepository contribuicaoRepository, AlunoService alunoService) {
        this.mesRepository = mesRepository;
        this.contribuicaoRepository = contribuicaoRepository;
        this.alunoService = alunoService;
    }

    @Override
    public Optional<MesDeContribuicoes> buscarPorDescricao(String descricao) {
        return mesRepository.findByDescricao(descricao).map(this::paraDominio);
    }

    @Override
    public Optional<MesDeContribuicoes> buscarParaEncerramento(String descricao) {
        // SELECT ... FOR UPDATE: o lock fica aqui, escondido atrás da porta.
        return mesRepository.findByDescricaoParaEncerramento(descricao).map(this::paraDominio);
    }

    @Override
    public MesDeContribuicoes buscarOuCriar(String descricao) {
        return paraDominio(buscarOuCriarEntity(descricao));
    }

    @Override
    public void salvar(MesDeContribuicoes mes) {
        MesDeContribuicoesEntity mesEntity = buscarOuCriarEntity(mes.getDescricao());

        // O domínio só acrescenta contribuições: grava apenas a "cauda" ainda não persistida.
        int jaPersistidas = contribuicaoRepository.findByMesOrderByIdAsc(mesEntity).size();
        List<Contribuicao> todas = mes.getContribuicoes();
        for (int i = jaPersistidas; i < todas.size(); i++) {
            Contribuicao nova = todas.get(i);
            AlunoEntity aluno = alunoService.buscarOuCriar(nova.getAluno().getNome());
            contribuicaoRepository.save(new ContribuicaoEntity(aluno, mesEntity, nova.getPeso()));
        }

        if (mes.isEncerrado() && !mesEntity.isEncerrado()) {
            AlunoEntity vencedorEntity = null;
            if (mes.getVencedor() != null) {
                // Escrita EXPLÍCITA do aluno (curso concedido): não depende de dirty checking.
                alunoService.salvar(mes.getVencedor());
                vencedorEntity = alunoService.buscarPorNome(mes.getVencedor().getNome());
            }
            mesEntity.encerrarCom(vencedorEntity);
            mesRepository.save(mesEntity);
        }
    }

    private MesDeContribuicoesEntity buscarOuCriarEntity(String descricao) {
        return mesRepository.findByDescricao(descricao)
                .orElseGet(() -> mesRepository.save(new MesDeContribuicoesEntity(descricao)));
    }

    private MesDeContribuicoes paraDominio(MesDeContribuicoesEntity mesEntity) {
        // Uma única instância de Aluno por nome: o vencedor é o mesmo objeto das contribuições.
        Map<String, Aluno> alunos = new HashMap<>();
        List<Contribuicao> contribuicoes = contribuicaoRepository.findByMesOrderByIdAsc(mesEntity).stream()
                .map(c -> new Contribuicao(alunoDe(alunos, c.getAluno()), c.getPeso()))
                .toList();
        Aluno vencedor = mesEntity.getVencedor() == null ? null : alunoDe(alunos, mesEntity.getVencedor());
        return MesDeContribuicoes.restaurar(mesEntity.getDescricao(), contribuicoes,
                mesEntity.isEncerrado(), vencedor, Sorteador.aleatorio());
    }

    private Aluno alunoDe(Map<String, Aluno> cache, AlunoEntity entity) {
        return cache.computeIfAbsent(entity.getNome(), nome -> entity.paraDominio());
    }
}
