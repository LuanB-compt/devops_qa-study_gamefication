package br.com.valueprojects.gamification;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.assinatura.domain.Assinatura;
import br.com.valueprojects.gamification.assinatura.dto.AssinaturaResponse;
import br.com.valueprojects.gamification.assinatura.service.AssinaturaService;
import br.com.valueprojects.gamification.fake.FakeAssinaturaPort;
import br.com.valueprojects.gamification.shared.RecursoNaoEncontradoException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Domínio de Assinatura (cenário 2) + caso de uso AssinaturaService com a porta
// trocada por um fake em memória (sem Spring, sem banco, sem Mockito).
public class AssinaturaTest {

    // Cenário 2: Dada uma mensalidade não paga e o aluno está ativo, quando
    // encerra o mês e não é realizado o pagamento, então a assinatura do
    // aluno é cancelada.
    @Test
    public void deveCancelarAssinaturaAtivaQuandoMesEncerraSemPagamento() {
        // Given
        Aluno aluno = new Aluno("Diana");
        Assinatura assinatura = new Assinatura(aluno);
        assertTrue(assinatura.isAtiva());

        // When (sem registrarPagamento())
        assinatura.encerrarMes();

        // Then
        assertFalse(assinatura.isAtiva());
    }

    // Complementar ao cenário 2: assinatura permanece ativa quando o
    // pagamento é realizado antes do encerramento do mês.
    @Test
    public void deveManterAssinaturaAtivaQuandoPagamentoForRealizado() {
        // Given
        Aluno aluno = new Aluno("Eduardo");
        Assinatura assinatura = new Assinatura(aluno);
        assinatura.registrarPagamento();
        assertTrue(assinatura.isPagamentoRealizado());

        // When
        assinatura.encerrarMes();

        // Then (e o pagamento é "zerado" para o mês seguinte)
        assertTrue(assinatura.isAtiva());
        assertFalse(assinatura.isPagamentoRealizado());
    }

    // BLUE - cobertura: getAluno() expõe o aluno vinculado à assinatura.
    @Test
    public void deveExporOAlunoVinculadoAAssinatura() {
        // Given
        Aluno aluno = new Aluno("Fernanda");
        Assinatura assinatura = new Assinatura(aluno);

        // Then
        assertEquals(aluno, assinatura.getAluno());
    }

    // BLUE - cobertura: encerrar o mês de uma assinatura já cancelada não deve
    // ter efeito (mantém inativa, sem lançar exceção).
    @Test
    public void deveManterAssinaturaJaCanceladaAoEncerrarNovamenteOMes() {
        // Given
        Aluno aluno = new Aluno("Gustavo");
        Assinatura assinatura = new Assinatura(aluno);
        assinatura.encerrarMes();
        assertFalse(assinatura.isAtiva());

        // When
        assinatura.encerrarMes();

        // Then
        assertFalse(assinatura.isAtiva());
    }

    // BLUE - cobertura: construtor de reidratação usado pelo adapter para
    // reconstruir o estado já persistido de uma assinatura.
    @Test
    public void deveReidratarAssinaturaComEstadoJaPersistido() {
        // Given / When
        Aluno aluno = new Aluno("Helio");
        Assinatura assinatura = new Assinatura(aluno, false, true);

        // Then
        assertEquals(aluno, assinatura.getAluno());
        assertFalse(assinatura.isAtiva());
        assertTrue(assinatura.isPagamentoRealizado());
    }

    // ---- Caso de uso (application service) com fake em memória ----

    private AssinaturaService assinaturaService;

    @BeforeEach
    void montar() {
        assinaturaService = new AssinaturaService(new FakeAssinaturaPort());
    }

    @Test
    public void deveCriarNovaAssinaturaAtivaQuandoAlunoAindaNaoTemUma() {
        AssinaturaResponse response = assinaturaService.criar("Leo");

        assertEquals("Leo", response.alunoNome());
        assertTrue(response.ativa());
        assertFalse(response.pagamentoRealizado());
        assertEquals(response, assinaturaService.consultar("Leo"));
    }

    @Test
    public void deveManterAssinaturaExistenteAoCriarParaAlunoJaAssinante() {
        // Given (assinatura já paga)
        assinaturaService.criar("Karla");
        assinaturaService.registrarPagamento("Karla");

        // When
        AssinaturaResponse response = assinaturaService.criar("Karla");

        // Then (não recria nem zera o estado)
        assertTrue(response.pagamentoRealizado());
    }

    @Test
    public void deveRegistrarPagamentoEPersistir() {
        assinaturaService.criar("Marcia");

        AssinaturaResponse response = assinaturaService.registrarPagamento("Marcia");

        assertTrue(response.pagamentoRealizado());
        assertTrue(assinaturaService.consultar("Marcia").pagamentoRealizado());
    }

    @Test
    public void deveCancelarERegistrarNoEncerramentoDoMesSemPagamento() {
        assinaturaService.criar("Nadia");

        AssinaturaResponse response = assinaturaService.encerrarMes("Nadia");

        assertFalse(response.ativa());
        assertFalse(assinaturaService.consultar("Nadia").ativa());
    }

    @Test
    public void deveManterAtivaEZerarPagamentoNoEncerramentoDoMesComPagamento() {
        assinaturaService.criar("Otavio");
        assinaturaService.registrarPagamento("Otavio");

        AssinaturaResponse response = assinaturaService.encerrarMes("Otavio");

        assertTrue(response.ativa());
        assertFalse(response.pagamentoRealizado());
    }

    @Test
    public void deveLancarExcecaoNeutraQuandoAssinaturaNaoEncontrada() {
        assertThrows(RecursoNaoEncontradoException.class, () -> assinaturaService.consultar("Quenia"));
        assertThrows(RecursoNaoEncontradoException.class, () -> assinaturaService.registrarPagamento("Quenia"));
        assertThrows(RecursoNaoEncontradoException.class, () -> assinaturaService.encerrarMes("Quenia"));
    }
}
