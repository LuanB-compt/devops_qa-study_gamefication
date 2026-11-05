package br.com.valueprojects.gamification.assinatura.service;

import br.com.valueprojects.gamification.assinatura.domain.Assinatura;
import java.util.Optional;

/** Porta de saída (persistência) do caso de uso de assinaturas, em termos de domínio. */
public interface AssinaturaPort {

    Optional<Assinatura> buscarPorAluno(String alunoNome);

    /** Cria ou atualiza (upsert) a assinatura do aluno, criando o aluno se necessário. */
    void salvar(Assinatura assinatura);
}
