package br.com.valueprojects.gamification.assinatura.service;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import br.com.valueprojects.gamification.assinatura.domain.Assinatura;
import br.com.valueprojects.gamification.assinatura.dto.AssinaturaResponse;
import br.com.valueprojects.gamification.assinatura.entity.AssinaturaEntity;
import br.com.valueprojects.gamification.assinatura.repository.AssinaturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

/**
 * Orquestra a persistência de {@link AssinaturaEntity}, reaproveitando
 * {@link Assinatura} do domínio (cenário 2 do bdd.md) para decidir se a
 * assinatura deve ser cancelada ao encerrar o mês sem pagamento. O estado
 * persistido é usado para reidratar o objeto de domínio a cada operação, via
 * o construtor {@link Assinatura#Assinatura(Aluno, boolean, boolean)}.
 */
@Service
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final AlunoService alunoService;

    public AssinaturaService(AssinaturaRepository assinaturaRepository, AlunoService alunoService) {
        this.assinaturaRepository = assinaturaRepository;
        this.alunoService = alunoService;
    }

    @Transactional
    public AssinaturaResponse criar(String alunoNome) {
        AlunoEntity alunoEntity = alunoService.buscarOuCriar(alunoNome);
        AssinaturaEntity entity = assinaturaRepository.findByAluno(alunoEntity)
                .orElseGet(() -> assinaturaRepository.save(new AssinaturaEntity(alunoEntity)));
        return paraResponse(entity);
    }

    @Transactional
    public AssinaturaResponse registrarPagamento(String alunoNome) {
        AssinaturaEntity entity = buscarOuFalhar(alunoNome);
        Assinatura assinaturaDominio = carregarDominio(entity);

        assinaturaDominio.registrarPagamento();
        entity.setPagamentoRealizado(true);

        return paraResponse(entity);
    }

    /**
     * Cenário 2: "Dada uma mensalidade não paga e o aluno está ativo, quando
     * encerra o mês e não é realizado o pagamento, então a assinatura do
     * aluno é cancelada".
     */
    @Transactional
    public AssinaturaResponse encerrarMes(String alunoNome) {
        AssinaturaEntity entity = buscarOuFalhar(alunoNome);
        Assinatura assinaturaDominio = carregarDominio(entity);

        assinaturaDominio.encerrarMes();
        entity.setAtiva(assinaturaDominio.isAtiva());
        entity.setPagamentoRealizado(false);

        return paraResponse(entity);
    }

    public AssinaturaResponse consultar(String alunoNome) {
        return paraResponse(buscarOuFalhar(alunoNome));
    }

    private Assinatura carregarDominio(AssinaturaEntity entity) {
        Aluno alunoDominio = new Aluno(entity.getAluno().getNome());
        return new Assinatura(alunoDominio, entity.isAtiva(), entity.isPagamentoRealizado());
    }

    private AssinaturaEntity buscarOuFalhar(String alunoNome) {
        AlunoEntity alunoEntity = alunoService.buscarPorNome(alunoNome);
        return assinaturaRepository.findByAluno(alunoEntity)
                .orElseThrow(() -> new EntityNotFoundException("Assinatura não encontrada para o aluno: " + alunoNome));
    }

    private AssinaturaResponse paraResponse(AssinaturaEntity entity) {
        return new AssinaturaResponse(entity.getAluno().getNome(), entity.isAtiva(), entity.isPagamentoRealizado());
    }
}
