package br.com.valueprojects.gamification;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.curso.service.CursoService;
import br.com.valueprojects.gamification.curso.domain.CursoConcluido;
import br.com.valueprojects.gamification.fake.FakeAlunoPort;
import br.com.valueprojects.gamification.fake.FakeCursoPort;
import br.com.valueprojects.gamification.fake.FakeEventoPublisherPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Caso de uso CursoService (cenário 5) sem Spring e sem banco: portas trocadas por
// fakes em memória. O bônus de +3 e a promoção a Premium são regras de Aluno (domínio);
// aqui se valida que o service orquestra e PERSISTE o resultado.
public class CursoTest {

    private FakeAlunoPort alunos;
    private FakeCursoPort cursos;
    private FakeEventoPublisherPort eventos;
    private CursoService cursoService;

    @BeforeEach
    void montar() {
        alunos = new FakeAlunoPort();
        cursos = new FakeCursoPort();
        eventos = new FakeEventoPublisherPort();
        cursoService = new CursoService(cursos, alunos, eventos);
    }

    // ASR-6: cada conclusão registra exatamente UM evento, descrevendo o que o domínio decidiu.
    @Test
    public void deveRegistrarUmEventoPorConclusaoComOResultadoDoDominio() {
        // When
        cursoService.concluirCurso("Kleber", "Docker", 9.0);

        // Then
        assertEquals(1, eventos.publicados().size());
        assertEquals(CursoConcluido.TIPO, eventos.publicados().get(0).tipo());
        CursoConcluido evento = (CursoConcluido) eventos.publicados().get(0).payload();
        assertEquals("Kleber", evento.alunoNome());
        assertEquals("Docker", evento.cursoNome());
        assertEquals(4, evento.cursosGanhos());
        assertEquals(4, evento.cursosConcluidosTotal());
    }

    @Test
    public void eventoDeveInformarPromocaoAPremium() {
        alunos.salvar(new Aluno("Lia", 11, 0, Aluno.Plano.BASICO));

        cursoService.concluirCurso("Lia", "Kubernetes", 6.0);

        assertTrue(((CursoConcluido) eventos.publicados().get(0).payload()).virouPremium());
    }

    // Fato sem evento nunca acontece: se salvar o aluno falha, o evento nem é registrado.
    @Test
    public void naoDeveRegistrarEventoQuandoASalvaDoAlunoFalha() {
        FakeAlunoPort quebrado = new FakeAlunoPort() {
            @Override
            public AlunoResponse salvar(Aluno aluno) {
                throw new IllegalStateException("banco indisponível");
            }
        };
        CursoService servico = new CursoService(cursos, quebrado, eventos);

        assertThrows(IllegalStateException.class, () -> servico.concluirCurso("Marta", "Git", 8.0));

        assertTrue(eventos.publicados().isEmpty());
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
