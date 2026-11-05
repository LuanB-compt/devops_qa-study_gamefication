package br.com.valueprojects.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.domain.ConclusaoDeCurso;
import br.com.valueprojects.gamification.aluno.vo.Curso;
import br.com.valueprojects.gamification.curso.domain.CursoConcluido;
import org.junit.jupiter.api.Test;

// O domínio devolve o FATO da conclusão (base do evento do ASR-6): quanto foi creditado
// e se houve promoção, sem que o service precise comparar "antes e depois".
public class ConclusaoDeCursoTest {

    @Test
    public void deveInformarCreditoSimplesQuandoMediaNaoUltrapassaSete() {
        Aluno aluno = new Aluno("Ana");

        ConclusaoDeCurso conclusao = aluno.concluirCurso(new Curso("Java"), 7.0);

        assertEquals(1, conclusao.cursosGanhos());
        assertFalse(conclusao.bonusDeMedia());
        assertFalse(conclusao.virouPremium());
        assertEquals("Java", conclusao.cursoNome());
    }

    @Test
    public void deveInformarBonusQuandoMediaAcimaDeSete() {
        Aluno aluno = new Aluno("Ana");

        ConclusaoDeCurso conclusao = aluno.concluirCurso(new Curso("Java"), 8.0);

        assertEquals(4, conclusao.cursosGanhos());
        assertTrue(conclusao.bonusDeMedia());
    }

    @Test
    public void deveInformarPromocaoAPremiumApenasNaConclusaoQueAProvoca() {
        Aluno aluno = new Aluno("Ana", 8, 0, Aluno.Plano.BASICO);

        ConclusaoDeCurso promovida = aluno.concluirCurso(new Curso("A"), 9.0); // 8 + 4 = 12
        ConclusaoDeCurso seguinte = aluno.concluirCurso(new Curso("B"), 5.0);

        assertTrue(promovida.virouPremium());
        assertFalse(seguinte.virouPremium());
    }

    @Test
    public void eventoDeveCarregarOTotalAcumuladoJaComAConclusao() {
        Aluno aluno = new Aluno("Ana", 2, 0, Aluno.Plano.BASICO);

        ConclusaoDeCurso conclusao = aluno.concluirCurso(new Curso("Java"), 9.0);
        CursoConcluido evento = CursoConcluido.de(aluno, conclusao);

        assertEquals("Ana", evento.alunoNome());
        assertEquals("Java", evento.cursoNome());
        assertEquals(4, evento.cursosGanhos());
        assertEquals(6, evento.cursosConcluidosTotal());
        assertFalse(evento.virouPremium());
    }
}
