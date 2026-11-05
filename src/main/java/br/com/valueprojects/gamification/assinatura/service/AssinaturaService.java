package br.com.valueprojects.gamification.assinatura.service;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.assinatura.domain.Assinatura;
import br.com.valueprojects.gamification.assinatura.dto.AssinaturaResponse;
import br.com.valueprojects.gamification.shared.RecursoNaoEncontradoException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service da assinatura (cenário 2). Orquestra: busca a
 * {@link Assinatura} pela porta, aciona o método de negócio do domínio e salva.
 * A decisão de cancelar (mês encerrado sem pagamento) é de {@link Assinatura}.
 */
@Service
public class AssinaturaService {

    private final AssinaturaPort assinaturaPort;

    public AssinaturaService(AssinaturaPort assinaturaPort) {
        this.assinaturaPort = assinaturaPort;
    }

    @Transactional
    public AssinaturaResponse criar(String alunoNome) {
        Assinatura assinatura = assinaturaPort.buscarPorAluno(alunoNome).orElseGet(() -> {
            Assinatura nova = new Assinatura(new Aluno(alunoNome));
            assinaturaPort.salvar(nova);
            return nova;
        });
        return paraResponse(assinatura);
    }

    @Transactional
    public AssinaturaResponse registrarPagamento(String alunoNome) {
        Assinatura assinatura = buscarOuFalhar(alunoNome);
        assinatura.registrarPagamento();
        assinaturaPort.salvar(assinatura);
        return paraResponse(assinatura);
    }

    /** Cenário 2: mensalidade não paga + mês encerrado => assinatura cancelada (regra no domínio). */
    @Transactional
    public AssinaturaResponse encerrarMes(String alunoNome) {
        Assinatura assinatura = buscarOuFalhar(alunoNome);
        assinatura.encerrarMes();
        assinaturaPort.salvar(assinatura);
        return paraResponse(assinatura);
    }

    public AssinaturaResponse consultar(String alunoNome) {
        return paraResponse(buscarOuFalhar(alunoNome));
    }

    private Assinatura buscarOuFalhar(String alunoNome) {
        return assinaturaPort.buscarPorAluno(alunoNome).orElseThrow(
                () -> new RecursoNaoEncontradoException("Assinatura não encontrada para o aluno: " + alunoNome));
    }

    private AssinaturaResponse paraResponse(Assinatura assinatura) {
        return new AssinaturaResponse(assinatura.getAluno().getNome(), assinatura.isAtiva(),
                assinatura.isPagamentoRealizado());
    }
}
