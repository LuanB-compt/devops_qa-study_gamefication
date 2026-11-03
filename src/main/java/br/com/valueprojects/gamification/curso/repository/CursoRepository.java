package br.com.valueprojects.gamification.curso.repository;

import br.com.valueprojects.gamification.curso.entity.CursoEntity;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CursoRepository extends JpaRepository<CursoEntity, Long> {

    Optional<CursoEntity> findByNome(String nome);
}
