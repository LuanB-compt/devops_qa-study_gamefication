package br.com.valueprojects.gamification.curso.service;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.aluno.service.AlunoPort;
import br.com.valueprojects.gamification.aluno.vo.Curso;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service da conclusão de curso (cenário 5). Só orquestra:
 * carrega o aluno (com o estado real), pede ao domínio que conclua o curso e
 * salva. O bônus de +3 cursos e a promoção a Premium com 12 cursos são regras de
 * {@link Aluno#concluirCurso(Curso, double)}, não deste serviço.
 */
@Service
public class CursoService {

    private final CursoPort cursoPort;
    private final AlunoPort alunoPort;

    public CursoService(CursoPort cursoPort, AlunoPort alunoPort) {
        this.cursoPort = cursoPort;
        this.alunoPort = alunoPort;
    }

    @Transactional
    public AlunoResponse concluirCurso(String alunoNome, String cursoNome, double media) {
        Aluno aluno = alunoPort.obterOuCriar(alunoNome);
        Curso curso = cursoPort.buscarOuRegistrar(cursoNome);
        aluno.concluirCurso(curso, media);
        return alunoPort.salvar(aluno);
    }
}
