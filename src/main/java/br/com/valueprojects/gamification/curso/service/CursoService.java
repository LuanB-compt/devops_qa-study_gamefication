package br.com.valueprojects.gamification.curso.service;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import br.com.valueprojects.gamification.aluno.vo.Curso;
import br.com.valueprojects.gamification.curso.entity.CursoEntity;
import br.com.valueprojects.gamification.curso.repository.CursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra a persistência de {@link CursoEntity} e a conclusão de cursos
 * por um aluno, reaproveitando {@link Aluno#concluirCurso(Curso, double)}
 * (cenário 5 do bdd.md) para decidir o bônus de +3 cursos quando a média é
 * maior que 7.
 */
@Service
public class CursoService {

    private static final int CURSOS_PARA_PREMIUM = 12;

    private final CursoRepository cursoRepository;
    private final AlunoService alunoService;

    public CursoService(CursoRepository cursoRepository, AlunoService alunoService) {
        this.cursoRepository = cursoRepository;
        this.alunoService = alunoService;
    }

    /**
     * Cenário 5: cria um {@link Aluno} de domínio "zerado" apenas para obter,
     * via {@link Aluno#concluirCurso(Curso, double)}, o delta de cursos
     * concedido por essa conclusão (1, ou 4 se a média for maior que 7); o
     * delta é então somado ao total já persistido do aluno.
     */
    @Transactional
    public AlunoResponse concluirCurso(String alunoNome, String cursoNome, double media) {
        AlunoEntity alunoEntity = alunoService.buscarOuCriar(alunoNome);
        CursoEntity cursoEntity = cursoRepository.findByNome(cursoNome)
                .orElseGet(() -> cursoRepository.save(new CursoEntity(cursoNome)));

        Aluno alunoDominio = new Aluno(alunoNome);
        Curso cursoDominio = new Curso(cursoEntity.getNome());
        alunoDominio.concluirCurso(cursoDominio, media);

        alunoEntity.setCursosConcluidos(alunoEntity.getCursosConcluidos() + alunoDominio.getCursosConcluidos());
        atualizarPlanoSeAtingiuDozeCursos(alunoEntity);

        return alunoService.paraResponse(alunoEntity);
    }

    /**
     * Regra do case (fora dos 5 cenários BDD, não implementada no domínio):
     * ao completar 12 cursos, o aluno vira Premium e ganha 3 moedas.
     */
    private void atualizarPlanoSeAtingiuDozeCursos(AlunoEntity aluno) {
        if (aluno.getPlano() == Aluno.Plano.BASICO && aluno.getCursosConcluidos() >= CURSOS_PARA_PREMIUM) {
            aluno.setPlano(Aluno.Plano.PREMIUM);
            aluno.setMoedas(aluno.getMoedas() + 3);
        }
    }
}
