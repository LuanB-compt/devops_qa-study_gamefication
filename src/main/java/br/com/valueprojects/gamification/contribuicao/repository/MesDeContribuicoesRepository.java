package br.com.valueprojects.gamification.contribuicao.repository;

import br.com.valueprojects.gamification.contribuicao.entity.MesDeContribuicoesEntity;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MesDeContribuicoesRepository extends JpaRepository<MesDeContribuicoesEntity, Long> {

    Optional<MesDeContribuicoesEntity> findByDescricao(String descricao);
}
