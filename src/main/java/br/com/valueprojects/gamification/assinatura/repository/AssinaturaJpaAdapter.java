package br.com.valueprojects.gamification.assinatura.repository;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import br.com.valueprojects.gamification.assinatura.domain.Assinatura;
import br.com.valueprojects.gamification.assinatura.entity.AssinaturaEntity;
import br.com.valueprojects.gamification.assinatura.service.AssinaturaPort;
import java.util.Optional;

import org.springframework.stereotype.Component;

/** Adapter JPA da {@link AssinaturaPort}: traduz {@link Assinatura} <-> {@link AssinaturaEntity}. */
@Component
public class AssinaturaJpaAdapter implements AssinaturaPort {

    private final AssinaturaRepository assinaturaRepository;
    private final AlunoService alunoService;

    public AssinaturaJpaAdapter(AssinaturaRepository assinaturaRepository, AlunoService alunoService) {
        this.assinaturaRepository = assinaturaRepository;
        this.alunoService = alunoService;
    }

    @Override
    public Optional<Assinatura> buscarPorAluno(String alunoNome) {
        // Consulta pelo nome do aluno (path aluno.nome): o adapter nem precisa carregar a AlunoEntity.
        return assinaturaRepository.findByAlunoNome(alunoNome)
                .map(e -> new Assinatura(new Aluno(e.getAluno().getNome()), e.isAtiva(), e.isPagamentoRealizado()));
    }

    @Override
    public void salvar(Assinatura assinatura) {
        String nome = assinatura.getAluno().getNome();
        AssinaturaEntity entity = assinaturaRepository.findByAlunoNome(nome)
                .orElseGet(() -> new AssinaturaEntity(alunoService.buscarOuCriar(nome)));
        entity.setAtiva(assinatura.isAtiva());
        entity.setPagamentoRealizado(assinatura.isPagamentoRealizado());
        assinaturaRepository.save(entity);
    }
}
