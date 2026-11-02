package br.com.valueprojects.gamification;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import br.com.valueprojects.gamification.assinatura.domain.Assinatura;
import br.com.valueprojects.gamification.assinatura.dto.AssinaturaResponse;
import br.com.valueprojects.gamification.assinatura.entity.AssinaturaEntity;
import br.com.valueprojects.gamification.assinatura.repository.AssinaturaRepository;
import br.com.valueprojects.gamification.assinatura.service.AssinaturaService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// RED - TDD
// Testes unitários (Mockito) da camada service
// Assinatura do domínio (cenário 2) a partir do estado persistido
// para decidir se a assinatura deve ser cancelada ao encerrar o mês.
@ExtendWith(MockitoExtension.class)
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

        // When
        assinatura.encerrarMes();

        // Then
        assertTrue(assinatura.isAtiva());
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

    // BLUE - cobertura: construtor de reidratação usado pelo service para
    // reconstruir o estado já persistido de uma assinatura.
    @Test
    public void deveReidratarAssinaturaComEstadoJaPersistido() {
        // Given / When
        Aluno aluno = new Aluno("Helio");
        Assinatura assinatura = new Assinatura(aluno, false, true);

        // Then
        assertEquals(aluno, assinatura.getAluno());
        assertFalse(assinatura.isAtiva());
    }

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private AlunoService alunoService;

    @InjectMocks
    private AssinaturaService assinaturaService;

    @Test
    public void deveRetornarAssinaturaExistenteAoCriarParaAlunoJaAssinante() {
        // Given
        AlunoEntity aluno = new AlunoEntity("Karla");
        AssinaturaEntity existente = new AssinaturaEntity(aluno);
        when(alunoService.buscarOuCriar("Karla")).thenReturn(aluno);
        when(assinaturaRepository.findByAluno(aluno)).thenReturn(Optional.of(existente));

        // When
        AssinaturaResponse response = assinaturaService.criar("Karla");

        // Then
        assertTrue(response.ativa());
        verify(assinaturaRepository, never()).save(any());
    }

    @Test
    public void deveCriarNovaAssinaturaAtivaQuandoAlunoAindaNaoTemUma() {
        // Given
        AlunoEntity aluno = new AlunoEntity("Leo");
        when(alunoService.buscarOuCriar("Leo")).thenReturn(aluno);
        when(assinaturaRepository.findByAluno(aluno)).thenReturn(Optional.empty());
        when(assinaturaRepository.save(any(AssinaturaEntity.class))).thenReturn(new AssinaturaEntity(aluno));

        // When
        AssinaturaResponse response = assinaturaService.criar("Leo");

        // Then
        assertEquals("Leo", response.alunoNome());
        assertTrue(response.ativa());
        assertFalse(response.pagamentoRealizado());
    }

    @Test
    public void deveRegistrarPagamentoDaAssinatura() {
        // Given
        AlunoEntity aluno = new AlunoEntity("Marcia");
        AssinaturaEntity entity = new AssinaturaEntity(aluno);
        when(alunoService.buscarPorNome("Marcia")).thenReturn(aluno);
        when(assinaturaRepository.findByAluno(aluno)).thenReturn(Optional.of(entity));

        // When
        AssinaturaResponse response = assinaturaService.registrarPagamento("Marcia");

        // Then
        assertTrue(response.pagamentoRealizado());
    }

    // Cenário 2: mensalidade não paga e aluno ativo -> assinatura cancelada ao encerrar o mês.
    @Test
    public void deveCancelarAssinaturaAoEncerrarMesSemPagamento() {
        // Given
        AlunoEntity aluno = new AlunoEntity("Nadia");
        AssinaturaEntity entity = new AssinaturaEntity(aluno); // ativa=true, pagamentoRealizado=false
        when(alunoService.buscarPorNome("Nadia")).thenReturn(aluno);
        when(assinaturaRepository.findByAluno(aluno)).thenReturn(Optional.of(entity));

        // When
        AssinaturaResponse response = assinaturaService.encerrarMes("Nadia");

        // Then
        assertFalse(response.ativa());
        assertFalse(response.pagamentoRealizado());
    }

    @Test
    public void naoDeveCancelarAssinaturaAoEncerrarMesComPagamentoRealizado() {
        // Given
        AlunoEntity aluno = new AlunoEntity("Otavio");
        AssinaturaEntity entity = new AssinaturaEntity(aluno);
        entity.setPagamentoRealizado(true);
        when(alunoService.buscarPorNome("Otavio")).thenReturn(aluno);
        when(assinaturaRepository.findByAluno(aluno)).thenReturn(Optional.of(entity));

        // When
        AssinaturaResponse response = assinaturaService.encerrarMes("Otavio");

        // Then
        assertTrue(response.ativa());
        assertFalse(response.pagamentoRealizado());
    }

    @Test
    public void deveConsultarAssinaturaExistente() {
        // Given
        AlunoEntity aluno = new AlunoEntity("Paulo");
        AssinaturaEntity entity = new AssinaturaEntity(aluno);
        when(alunoService.buscarPorNome("Paulo")).thenReturn(aluno);
        when(assinaturaRepository.findByAluno(aluno)).thenReturn(Optional.of(entity));

        // When
        AssinaturaResponse response = assinaturaService.consultar("Paulo");

        // Then
        assertEquals("Paulo", response.alunoNome());
    }

    @Test
    public void deveLancarExcecaoQuandoAssinaturaNaoEncontradaParaOAluno() {
        // Given
        AlunoEntity aluno = new AlunoEntity("Quenia");
        when(alunoService.buscarPorNome("Quenia")).thenReturn(aluno);
        when(assinaturaRepository.findByAluno(aluno)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(EntityNotFoundException.class, () -> assinaturaService.consultar("Quenia"));
    }
}
