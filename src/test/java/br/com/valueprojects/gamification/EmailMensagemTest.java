package br.com.valueprojects.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.valueprojects.gamification.curso.domain.CursoConcluido;
import br.com.valueprojects.gamification.notificacao.domain.EmailMensagem;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class EmailMensagemTest {

    private static final UUID ID = UUID.randomUUID();

    private CursoConcluido evento(String aluno, boolean premium) {
        return new CursoConcluido(aluno, "Docker", 9.0, 4, 12, premium);
    }

    @Test
    public void deveComporEmailComChaveDeIdempotenciaDoEvento() {
        EmailMensagem m = EmailMensagem.paraCursoConcluido(ID, evento("Ana", false));

        assertEquals(ID, m.idempotencyKey());
        assertEquals("Curso concluído: Docker", m.assunto());
        assertTrue(m.corpo().contains("Docker"));
        assertTrue(m.corpo().contains("média 9.0"));
        assertTrue(m.corpo().contains("Cursos creditados: 4 (total: 12)"));
        assertFalse(m.corpo().contains("Premium"));
    }

    @Test
    public void deveMencionarPremiumQuandoAConclusaoPromoveOAluno() {
        EmailMensagem m = EmailMensagem.paraCursoConcluido(ID, evento("Ana", true));

        assertTrue(m.corpo().contains("Premium"));
    }

    @Test
    public void deveDerivarEnderecoSemAcentoNemEspaco() {
        EmailMensagem m = EmailMensagem.paraCursoConcluido(ID, evento("  José da Silva ", false));

        assertEquals("jose.da.silva@alunos.gamification.local", m.para());
    }
}
