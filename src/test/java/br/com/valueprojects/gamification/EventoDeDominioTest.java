package br.com.valueprojects.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.valueprojects.gamification.evento.domain.EventoDeDominio;
import br.com.valueprojects.gamification.evento.domain.EventoDeDominio.Status;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

// Ciclo de vida do evento na outbox: puro, sem Spring e sem banco.
public class EventoDeDominioTest {

    private static final Instant T0 = Instant.parse("2026-11-04T12:00:00Z");

    private EventoDeDominio novo() {
        return EventoDeDominio.novo("CURSO_CONCLUIDO", "{}", T0);
    }

    @Test
    public void eventoNovoNasceNaoEntreguePendenteEElegivelAgora() {
        EventoDeDominio e = novo();

        assertNotNull(e.getId());
        assertEquals(Status.PENDENTE, e.getStatus());
        assertEquals(0, e.getTentativas());
        assertTrue(e.estaProntoParaEntrega(T0));
    }

    @Test
    public void eventoEnviadoNaoEstaMaisProntoEnemPodeFalhar() {
        EventoDeDominio e = novo();

        e.marcarEnviado();

        assertEquals(Status.ENVIADO, e.getStatus());
        assertFalse(e.estaProntoParaEntrega(T0));
        // reentrega de quem já foi entregue é erro de programação, não deve passar calada
        assertThrows(IllegalStateException.class, e::marcarEnviado);
        assertThrows(IllegalStateException.class, () -> e.registrarFalha("x", T0));
    }

    @Test
    public void falhaReagendaComBackoffExponencial() {
        EventoDeDominio e = novo();

        e.registrarFalha("timeout", T0);
        assertEquals(T0.plus(Duration.ofSeconds(30)), e.getProximaTentativaEm());
        assertFalse(e.estaProntoParaEntrega(T0.plusSeconds(29)));
        assertTrue(e.estaProntoParaEntrega(T0.plusSeconds(30)));

        e.registrarFalha("timeout", T0.plusSeconds(30));
        assertEquals(T0.plusSeconds(30).plusSeconds(60), e.getProximaTentativaEm());
        assertEquals(Status.PENDENTE, e.getStatus());
    }

    @Test
    public void aoEsgotarTentativasViraFalhouESaiDaFila() {
        EventoDeDominio e = novo();

        for (int i = 0; i < EventoDeDominio.MAX_TENTATIVAS; i++) {
            e.registrarFalha("sem resposta", T0);
        }

        assertEquals(Status.FALHOU, e.getStatus());
        assertEquals(EventoDeDominio.MAX_TENTATIVAS, e.getTentativas());
        assertFalse(e.estaProntoParaEntrega(T0.plus(Duration.ofDays(1))));
    }

    @Test
    public void sucessoAposFalhaLimpaOUltimoErro() {
        EventoDeDominio e = novo();
        e.registrarFalha("timeout", T0);

        e.marcarEnviado();

        assertNull(e.getUltimoErro());
    }

    @Test
    public void erroMuitoLongoETruncadoParaCaberNaColuna() {
        EventoDeDominio e = novo();

        e.registrarFalha("x".repeat(2000), T0);

        assertEquals(500, e.getUltimoErro().length());
    }

    @Test
    public void erroNuloViraMensagemPadrao() {
        EventoDeDominio e = novo();

        e.registrarFalha(null, T0);

        assertEquals("erro desconhecido", e.getUltimoErro());
    }
}
