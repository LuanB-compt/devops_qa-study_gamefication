package br.com.valueprojects.gamification.curso.repository;

import br.com.valueprojects.gamification.aluno.vo.Curso;
import br.com.valueprojects.gamification.curso.entity.CursoEntity;
import br.com.valueprojects.gamification.curso.service.CursoPort;

import org.springframework.stereotype.Component;

/** Adapter JPA da {@link CursoPort}. */
@Component
public class CursoJpaAdapter implements CursoPort {

    private final CursoRepository cursoRepository;

    public CursoJpaAdapter(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    @Override
    public Curso buscarOuRegistrar(String nome) {
        CursoEntity entity = cursoRepository.findByNome(nome)
                .orElseGet(() -> cursoRepository.save(new CursoEntity(nome)));
        return new Curso(entity.getNome());
    }
}
