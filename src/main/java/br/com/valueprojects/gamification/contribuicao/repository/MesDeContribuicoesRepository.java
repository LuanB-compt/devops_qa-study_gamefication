package br.com.valueprojects.gamification.contribuicao.repository;

import br.com.valueprojects.gamification.contribuicao.entity.MesDeContribuicoesEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MesDeContribuicoesRepository extends JpaRepository<MesDeContribuicoesEntity, Long> {

    Optional<MesDeContribuicoesEntity> findByDescricao(String descricao);

    /**
     * SELECT ... FOR UPDATE: duas requisições simultâneas de "encerrar" para o
     * mesmo mês são serializadas — a segunda espera a primeira commitar e já
     * enxerga {@code encerrado = true}. Sem o lock, ambas leriam "aberto" e o
     * vencedor ganharia o curso duas vezes (check-then-act).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MesDeContribuicoesEntity m where m.descricao = :descricao")
    Optional<MesDeContribuicoesEntity> findByDescricaoParaEncerramento(@Param("descricao") String descricao);
}
