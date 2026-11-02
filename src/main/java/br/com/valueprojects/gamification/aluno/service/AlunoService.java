package br.com.valueprojects.gamification.aluno.service;

import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import br.com.valueprojects.gamification.aluno.repository.AlunoRepository;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

/**
 * Orquestra a persistência de {@link AlunoEntity}. Não duplica regra de
 * negócio do domínio — apenas busca/cria alunos e traduz para
 * {@link AlunoResponse}; a lógica dos cenários fica nos demais services, que
 * reaproveitam as classes de {@code dominio}.
 */
@Service
public class AlunoService {

    private final AlunoRepository alunoRepository;

    public AlunoService(AlunoRepository alunoRepository) {
        this.alunoRepository = alunoRepository;
    }

    public AlunoEntity buscarOuCriar(String nome) {
        return alunoRepository.findByNome(nome)
                .orElseGet(() -> alunoRepository.save(new AlunoEntity(nome)));
    }

    public AlunoEntity buscarPorNome(String nome) {
        return alunoRepository.findByNome(nome)
                .orElseThrow(() -> new EntityNotFoundException("Aluno não encontrado: " + nome));
    }

    public List<AlunoEntity> listarTodos() {
        return alunoRepository.findAll();
    }

    public AlunoResponse paraResponse(AlunoEntity entity) {
        return new AlunoResponse(
                entity.getId(),
                entity.getNome(),
                entity.getCursosConcluidos(),
                entity.getMoedas(),
                entity.getPlano().name());
    }
}
