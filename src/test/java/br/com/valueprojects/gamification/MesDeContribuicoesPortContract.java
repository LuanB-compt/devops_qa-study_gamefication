package br.com.valueprojects.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.service.AlunoPort;
import br.com.valueprojects.gamification.contribuicao.domain.Contribuicao;
import br.com.valueprojects.gamification.contribuicao.domain.MesDeContribuicoes;
import br.com.valueprojects.gamification.contribuicao.service.MesDeContribuicoesPort;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suíte de CONTRATO da {@link MesDeContribuicoesPort}. É executada contra o
 * fake em memória e contra o adapter JPA (subclasses). Garante que os dois
 * adapters são intercambiáveis: se o fake divergir do banco, um deles reprova.
 */
// @Transactional aqui (e não só no @DataJpaTest da subclasse): o Spring não enxerga o da
// subclasse para métodos @Test herdados, e sem transação o TestEntityManager falha.
@Transactional
abstract class MesDeContribuicoesPortContract {

    protected abstract MesDeContribuicoesPort porta();

    protected abstract AlunoPort alunos();

    /** O adapter JPA precisa limpar o cache de 1º nível para provar que leu do banco. */
    protected void forcarLeituraDoBanco() {
        // fake: nada a fazer
    }

    @Test
    public void deveDevolverVazioParaMesInexistente() {
        assertTrue(porta().buscarPorDescricao("Nao/Existe").isEmpty());
        assertTrue(porta().buscarParaEncerramento("Nao/Existe").isEmpty());
    }

    @Test
    public void deveCriarMesAbertoSemContribuicoes() {
        MesDeContribuicoes mes = porta().buscarOuCriar("Mes/Novo");
        forcarLeituraDoBanco();

        MesDeContribuicoes lido = porta().buscarPorDescricao("Mes/Novo").orElseThrow();
        assertEquals("Mes/Novo", lido.getDescricao());
        assertFalse(lido.isEncerrado());
        assertTrue(lido.getContribuicoes().isEmpty());
        assertNull(mes.getVencedor());
    }

    @Test
    public void deveGravarSomenteAsContribuicoesNovasEPreservarAOrdem() {
        // Given
        MesDeContribuicoes mes = porta().buscarOuCriar("Mes/Ordem");
        mes.registrar(new Contribuicao(alunos().obterOuCriar("Ana"), 2));
        porta().salvar(mes);
        forcarLeituraDoBanco();

        // When (relê, acrescenta uma e salva de novo)
        MesDeContribuicoes relido = porta().buscarPorDescricao("Mes/Ordem").orElseThrow();
        relido.registrar(new Contribuicao(alunos().obterOuCriar("Bia"), 3));
        porta().salvar(relido);
        forcarLeituraDoBanco();

        // Then (sem duplicar a primeira)
        MesDeContribuicoes final_ = porta().buscarPorDescricao("Mes/Ordem").orElseThrow();
        assertEquals(2, final_.getContribuicoes().size());
        assertEquals("Ana", final_.getContribuicoes().get(0).getAluno().getNome());
        assertEquals(3.0, final_.getContribuicoes().get(1).getPeso());
    }

    @Test
    public void deveGravarVencedorEoCursoConcedidoDeFormaExplicita() {
        // Given
        MesDeContribuicoes mes = porta().buscarOuCriar("Mes/Apurado");
        mes.registrar(new Contribuicao(alunos().obterOuCriar("Ana"), 1));
        porta().salvar(mes);
        forcarLeituraDoBanco();

        // When
        MesDeContribuicoes aberto = porta().buscarParaEncerramento("Mes/Apurado").orElseThrow();
        aberto.encerrar();
        porta().salvar(aberto);
        forcarLeituraDoBanco();

        // Then (estado do mês e do aluno persistidos)
        MesDeContribuicoes lido = porta().buscarPorDescricao("Mes/Apurado").orElseThrow();
        assertTrue(lido.isEncerrado());
        assertEquals(new Aluno("Ana"), lido.getVencedor());
        assertEquals(1, lido.getVencedor().getCursosConcluidos());
        assertEquals(1, alunos().obter("Ana").getCursosConcluidos());
    }

    @Test
    public void deveGravarMesEncerradoSemVencedor() {
        MesDeContribuicoes mes = porta().buscarOuCriar("Mes/Vazio");
        mes.encerrar();
        porta().salvar(mes);
        forcarLeituraDoBanco();

        MesDeContribuicoes lido = porta().buscarPorDescricao("Mes/Vazio").orElseThrow();
        assertTrue(lido.isEncerrado());
        assertNull(lido.getVencedor());
    }
}
