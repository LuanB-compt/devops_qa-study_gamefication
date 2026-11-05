package br.com.valueprojects.gamification.fake;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.assinatura.domain.Assinatura;
import br.com.valueprojects.gamification.assinatura.service.AssinaturaPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Adapter em memória da {@link AssinaturaPort}; guarda o estado e reidrata um novo objeto a cada leitura. */
public class FakeAssinaturaPort implements AssinaturaPort {

    private record Estado(boolean ativa, boolean pagamentoRealizado) { }

    private final Map<String, Estado> assinaturas = new HashMap<>();

    @Override
    public Optional<Assinatura> buscarPorAluno(String alunoNome) {
        return Optional.ofNullable(assinaturas.get(alunoNome))
                .map(e -> new Assinatura(new Aluno(alunoNome), e.ativa(), e.pagamentoRealizado()));
    }

    @Override
    public void salvar(Assinatura assinatura) {
        assinaturas.put(assinatura.getAluno().getNome(),
                new Estado(assinatura.isAtiva(), assinatura.isPagamentoRealizado()));
    }
}
