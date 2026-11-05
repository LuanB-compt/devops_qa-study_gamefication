package br.com.valueprojects.gamification.assinatura.repository;

import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import br.com.valueprojects.gamification.assinatura.entity.AssinaturaEntity;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssinaturaRepository extends JpaRepository<AssinaturaEntity, Long> {

    Optional<AssinaturaEntity> findByAluno(AlunoEntity aluno);

    Optional<AssinaturaEntity> findByAlunoNome(String nome);
}
