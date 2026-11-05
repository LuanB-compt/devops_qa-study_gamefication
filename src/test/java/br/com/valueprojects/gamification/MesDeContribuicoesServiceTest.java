package br.com.valueprojects.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.valueprojects.gamification.contribuicao.dto.RankingMesResponse;
import br.com.valueprojects.gamification.contribuicao.dto.VencedorResponse;
import br.com.valueprojects.gamification.contribuicao.service.MesDeContribuicoesService;
import br.com.valueprojects.gamification.fake.FakeAlunoPort;
import br.com.valueprojects.gamification.fake.FakeMesDeContribuicoesPort;
import br.com.valueprojects.gamification.shared.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Mini-lab do Desafio 2: o MESMO caso de uso roda sem Spring e sem banco, com o
 * adapter trocado por um fake em memória e o sorteio controlado. Prova que as
 * regras estão no domínio e que o service só orquestra.
 */
public class MesDeContribuicoesServiceTest {

    private FakeAlunoPort alunos;
    private MesDeContribuicoesService service;

    @BeforeEach
    void montar() {
        alunos = new FakeAlunoPort();
        // sorteio determinístico: sempre o primeiro empatado
        service = new MesDeContribuicoesService(new FakeMesDeContribuicoesPort(alunos, limite -> 0), alunos);
    }

    @Test
    public void deveRetornarRankingPorSomaDePesosEmOrdemDecrescente() {
        // Given
        service.registrarContribuicao("Out/2026", "Ana", 1);
        service.registrarContribuicao("Out/2026", "Bia", 10);
        service.registrarContribuicao("Out/2026", "Ana", 2);

        // When
        RankingMesResponse ranking = service.consultarRanking("Out/2026");

        // Then (Bia 10, Ana 3)
        assertEquals("Bia", ranking.contribuicoes().get(0).alunoNome());
        assertEquals(10.0, ranking.contribuicoes().get(0).pontos());
        assertEquals("Ana", ranking.contribuicoes().get(1).alunoNome());
        assertEquals(3.0, ranking.contribuicoes().get(1).pontos());
    }

    @Test
    public void deveRecusarContribuicaoConsecutivaDoMesmoAluno() {
        service.registrarContribuicao("Out/2026", "Ana", 1);

        assertFalse(service.registrarContribuicao("Out/2026", "Ana", 1));
        assertEquals(1, service.consultarRanking("Out/2026").contribuicoes().size());
    }

    @Test
    public void deveConcederCursoAoVencedorEPersistirOEstadoDoAluno() {
        // Given
        service.registrarContribuicao("Out/2026", "Ana", 1);
        service.registrarContribuicao("Out/2026", "Bia", 5);

        // When
        VencedorResponse vencedor = service.encerrarMes("Out/2026");

        // Then (o curso foi ESCRITO no aluno, não só calculado em memória)
        assertEquals("Bia", vencedor.alunoVencedor());
        assertEquals(1, vencedor.cursosConcluidos());
        assertEquals(1, alunos.obter("Bia").getCursosConcluidos());
        assertEquals(0, alunos.obter("Ana").getCursosConcluidos());
    }

    @Test
    public void encerrarDuasVezesDeveSerIdempotenteComSorteioControlado() {
        // Given (empate: o sorteador do teste escolhe sempre o primeiro)
        service.registrarContribuicao("Out/2026", "Ana", 1);
        service.registrarContribuicao("Out/2026", "Bia", 1);

        // When
        VencedorResponse primeira = service.encerrarMes("Out/2026");
        VencedorResponse segunda = service.encerrarMes("Out/2026");

        // Then
        assertEquals("Ana", primeira.alunoVencedor());
        assertEquals(primeira, segunda);
        assertEquals(1, alunos.obter("Ana").getCursosConcluidos());
    }

    @Test
    public void deveEncerrarMesSemContribuicoesSemVencedor() {
        service.buscarOuCriarMes("Vazio/2026");

        VencedorResponse resposta = service.encerrarMes("Vazio/2026");

        assertNull(resposta.alunoVencedor());
        assertEquals(0, resposta.cursosConcluidos());
    }

    @Test
    public void naoDeveAceitarContribuicaoNemCriarAlunoEmMesEncerrado() {
        // Given
        service.registrarContribuicao("Out/2026", "Ana", 1);
        service.encerrarMes("Out/2026");

        // When
        boolean registrada = service.registrarContribuicao("Out/2026", "Felipe", 1);

        // Then
        assertFalse(registrada);
        assertFalse(alunos.existe("Felipe"));
        assertTrue(service.consultarRanking("Out/2026").contribuicoes().size() == 1);
    }

    @Test
    public void deveFalharComExcecaoNeutraQuandoMesNaoExiste() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.consultarRanking("Nao/Existe"));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.encerrarMes("Nao/Existe"));
    }
}
