package br.com.valueprojects.gamification.contribuicao.service;

import br.com.valueprojects.gamification.contribuicao.domain.MesDeContribuicoes;
import java.util.Optional;

/**
 * Porta de saída (persistência) do caso de uso de contribuições. Fala SÓ em
 * termos de domínio: nenhuma entity, repository ou exceção do JPA aparece aqui.
 * Quem a implementa (adapter JPA, fake em memória) decide como guardar.
 */
public interface MesDeContribuicoesPort {

    Optional<MesDeContribuicoes> buscarPorDescricao(String descricao);

    /**
     * Mesma busca, mas com a garantia de que duas apurações simultâneas do mesmo
     * mês serão serializadas. O mecanismo (SELECT ... FOR UPDATE) é detalhe do adapter.
     */
    Optional<MesDeContribuicoes> buscarParaEncerramento(String descricao);

    MesDeContribuicoes buscarOuCriar(String descricao);

    /**
     * Persiste o estado do mês. O domínio só ACRESCENTA contribuições, então o
     * adapter grava apenas as novas, além do resultado da apuração (se encerrado).
     */
    void salvar(MesDeContribuicoes mes);
}
