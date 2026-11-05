package br.com.valueprojects.gamification;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.curso.service.CursoService;
import br.com.valueprojects.gamification.fake.FakeAlunoPort;
import br.com.valueprojects.gamification.fake.FakeCursoPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Caso de uso CursoService (cenário 5) sem Spring e sem banco: portas trocadas por
// fakes em memória. O bônus de +3 e a promoção a Premium são regras de Aluno (domínio);
// aqui se valida que o service orquestra e PERSISTE o resultado.
public class CursoTest {

    private FakeAlunoPort alunos;
    private FakeCursoPort cursos;
    private CursoService cursoService;

    @BeforeEach
    void montar() {
        alunos = new FakeAlunoPort();
        cursos = new FakeCursoPort();
        cursoService = new CursoService(cursos, alunos);
    }

    @Test
    public void deveConcluirCursoSemBonusQuandoMediaNaoUltrapassaSete() {
        // When
        AlunoResponse response = cursoService.concluirCurso("Gustavo", "Java Básico", 7.0);

        // Then (só o curso concluído, sem bônus)
        assertEquals(1, response.cursosConcluidos());
        assertEquals("BASICO", response.plano());
        assertEquals(1, alunos.obter("Gustavo").getCursosConcluidos());
        assertTrue(cursos.catalogo().contains("Java Básico"));
    }

    @Test
    public void deveConcederBonusQuandoMediaAcimaDeSete() {
        // When
        AlunoResponse response = cursoService.concluirCurso("Helena", "Docker", 9.0);

        // Then (1 do curso + 3 de bônus pela média > 7)
        assertEquals(4, response.cursosConcluidos());
        assertEquals(4, alunos.obter("Helena").getCursosConcluidos());
    }

    @Test
    public void deveAcumularSobreOEstadoJaPersistidoDoAluno() {
        // Given
        cursoService.concluirCurso("Ivo", "A", 5.0);

        // When
        AlunoResponse response = cursoService.concluirCurso("Ivo", "B", 5.0);

        // Then
        assertEquals(2, response.cursosConcluidos());
    }

    @Test
    public void deveVirarPremiumEGanharMoedasAoAtingirDozeCursos() {
        // Given: aluno faltando 1 curso para os 12 necessários ao plano Premium
        alunos.salvar(new Aluno("Igor", 11, 0, Aluno.Plano.BASICO));

        // When (média <= 7, então só +1 curso, mas isso já fecha os 12)
        AlunoResponse response = cursoService.concluirCurso("Igor", "Kubernetes", 6.0);

        // Then
        assertEquals(12, response.cursosConcluidos());
        assertEquals("PREMIUM", response.plano());
        assertEquals(3, response.moedas());
    }

    @Test
    public void naoDeveDarMoedasDeNovoQuandoJaEhPremium() {
        // Given: aluno já Premium
        alunos.salvar(new Aluno("Julia", 12, 3, Aluno.Plano.PREMIUM));

        // When
        AlunoResponse response = cursoService.concluirCurso("Julia", "Terraform", 5.0);

        // Then (moedas não dobram, plano continua Premium)
        assertEquals(3, response.moedas());
        assertEquals("PREMIUM", response.plano());
    }
}
