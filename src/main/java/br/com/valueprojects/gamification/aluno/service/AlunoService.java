package br.com.valueprojects.gamification.aluno.service;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import br.com.valueprojects.gamification.aluno.repository.AlunoRepository;
import br.com.valueprojects.gamification.shared.RecursoNaoEncontradoException;
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
public class AlunoService implements AlunoPort {

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

    @Override
    public Aluno obterOuCriar(String nome) {
        return buscarOuCriar(nome).paraDominio();
    }

    @Override
    public Aluno obter(String nome) {
        return alunoRepository.findByNome(nome)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado: " + nome))
                .paraDominio();
    }

    @Override
    public AlunoResponse salvar(Aluno aluno) {
        AlunoEntity entity = alunoRepository.findByNome(aluno.getNome())
                .orElseGet(() -> new AlunoEntity(aluno.getNome()));
        entity.atualizarDe(aluno);
        return paraResponse(alunoRepository.save(entity));
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
