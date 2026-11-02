package br.com.valueprojects.gamification.contribuicao.repository;

import br.com.valueprojects.gamification.contribuicao.domain.MesDeContribuicoes;
import br.com.valueprojects.gamification.contribuicao.entity.ContribuicaoEntity;
import br.com.valueprojects.gamification.contribuicao.entity.MesDeContribuicoesEntity;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContribuicaoRepository extends JpaRepository<ContribuicaoEntity, Long> {

    /**
     * Ordenado por id crescente para preservar a ordem de registro, usada
     * pelo service ao reconstruir o MesDeContribuicoes do domínio (cenário 4
     * depende de saber qual foi a última contribuição registrada).
     */
    List<ContribuicaoEntity> findByMesOrderByIdAsc(MesDeContribuicoesEntity mes);
}
