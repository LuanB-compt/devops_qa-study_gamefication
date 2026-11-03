package br.com.valueprojects.gamification.aluno.repository;

import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlunoRepository extends JpaRepository<AlunoEntity, Long> {

    Optional<AlunoEntity> findByNome(String nome);
}
