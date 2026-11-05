package br.com.valueprojects.gamification;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import br.com.valueprojects.gamification.aluno.repository.AlunoRepository;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import br.com.valueprojects.gamification.aluno.vo.Curso;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.valueprojects.gamification.shared.RecursoNaoEncontradoException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// RED - TDD
// RED - TDD
// Testes unitários (Mockito) da camada service - AlunoService não duplica
// regra de negócio do domínio, então aqui validamos apenas a orquestração
// com o repositório e a tradução para AlunoResponse.
@ExtendWith(MockitoExtension.class)
public class AlunoTest {

    // Cenário 1: Dado o aluno com mais contribuições, quando encerra o mês,
    // então o Aluno ganha um curso.
    @Test
    public void deveGanharCursoQuandoTemMaisContribuicoesNoMes() {
        // Given
        Aluno aluno = new Aluno("Ana");
        int cursosAntes = aluno.getCursosConcluidos();

        // When
        aluno.ganharCurso();

        // Then
        assertEquals(cursosAntes + 1, aluno.getCursosConcluidos());
    }

    // Cenário 4: Dado um aluno que finalizou um curso e está com a média acima
    // de 7, quando finalizar o curso, então esse aluno ganhará mais 3 cursos.
    @Test
    public void deveGanharMaisTresCursosQuandoConcluiCursoComMediaAcimaDeSete() {
        // Given
        Aluno aluno = new Aluno("Bruno");
        Curso curso = new Curso("Docker na prática");
        int cursosAntes = aluno.getCursosConcluidos();

        // When
        aluno.concluirCurso(curso, 8.5);

        // Then (o curso concluído conta 1 + bônus de 3 pela média acima de 7)
        assertEquals(cursosAntes + 4, aluno.getCursosConcluidos());
    }

    // Complementar ao cenário 4: média igual ou abaixo de 7 não concede cursos extras.
    @Test
    public void naoDeveGanharCursosExtrasQuandoMediaNaoUltrapassaSete() {
        // Given
        Aluno aluno = new Aluno("Carlos");
        Curso curso = new Curso("Java Básico");
        int cursosAntes = aluno.getCursosConcluidos();

        // When
        aluno.concluirCurso(curso, 7.0);

        // Then (apenas o curso concluído é contabilizado, sem bônus)
        assertEquals(cursosAntes + 1, aluno.getCursosConcluidos());
    }

    // BLUE - cobertura: estado inicial de um aluno recém-criado (nome, moedas
    // e plano padrão), usado como base pelos cenários 2 (assinatura/plano) e
    // futura conversão de moedas.
    @Test
    public void deveIniciarComNomeMoedasZeroEPlanoBasico() {
        // Given / When
        Aluno aluno = new Aluno("Nina");

        // Then
        assertEquals("Nina", aluno.getNome());
        assertEquals(0, aluno.getMoedas());
        assertEquals(Aluno.Plano.BASICO, aluno.getPlano());
    }

    // BLUE - cobertura: equals/hashCode usados pela apuração de vencedores em
    // MesDeContribuicoes (comparação de alunos pelo nome).
    @Test
    public void deveSerIgualAOutroAlunoComMesmoNome() {
        Aluno aluno1 = new Aluno("Otavio");
        Aluno aluno2 = new Aluno("Otavio");

        assertTrue(aluno1.equals(aluno1));
        assertTrue(aluno1.equals(aluno2));
        assertEquals(aluno1.hashCode(), aluno2.hashCode());
    }

    @Test
    public void naoDeveSerIgualANuloOuAOutroTipoOuANomeDiferente() {
        Aluno aluno = new Aluno("Paula");

        assertFalse(aluno.equals(null));
        assertFalse(aluno.equals("Paula"));
        assertNotEquals(aluno, new Aluno("Quenia"));
    }

    @Test
    public void deveTratarNomeNuloAoCompararEHashear() {
        Aluno alunoSemNome1 = new Aluno(null);
        Aluno alunoSemNome2 = new Aluno(null);
        Aluno alunoComNome = new Aluno("Rafael");

        assertTrue(alunoSemNome1.equals(alunoSemNome2));
        assertFalse(alunoSemNome1.equals(alunoComNome));
        assertEquals(0, alunoSemNome1.hashCode());
    }

    // Cenário 4: Dado um aluno que finalizou um curso e está com a média
    // acima de 7, quando finalizar o curso, então esse aluno ganhará mais 3
    // cursos.
    @Test
    public void deveIdentificarCursoConcluidoComMediaAcimaDeSete() {
        // Given
        Aluno aluno = new Aluno("Fábio");
        Curso curso = new Curso("Spring Boot Avançado");
        int cursosAntes = aluno.getCursosConcluidos();

        // When
        aluno.concluirCurso(curso, 9.0);

        // Then
        assertEquals("Spring Boot Avançado", curso.getNome());
        assertEquals(cursosAntes + 4, aluno.getCursosConcluidos());
    }

    @Mock
    private AlunoRepository alunoRepository;

    @InjectMocks
    private AlunoService alunoService;

    @Test
    public void deveRetornarAlunoExistenteQuandoBuscarOuCriarEJaExiste() {
        // Given
        AlunoEntity existente = new AlunoEntity("Ana");
        when(alunoRepository.findByNome("Ana")).thenReturn(Optional.of(existente));

        // When
        AlunoEntity resultado = alunoService.buscarOuCriar("Ana");

        // Then
        assertSame(existente, resultado);
        verify(alunoRepository, never()).save(any());
    }

    @Test
    public void deveCriarNovoAlunoQuandoBuscarOuCriarENaoExiste() {
        // Given
        when(alunoRepository.findByNome("Bruno")).thenReturn(Optional.empty());
        AlunoEntity novo = new AlunoEntity("Bruno");
        when(alunoRepository.save(any(AlunoEntity.class))).thenReturn(novo);

        // When
        AlunoEntity resultado = alunoService.buscarOuCriar("Bruno");

        // Then
        assertEquals("Bruno", resultado.getNome());
        verify(alunoRepository).save(any(AlunoEntity.class));
    }

    @Test
    public void deveRetornarAlunoQuandoBuscarPorNomeExistente() {
        // Given
        AlunoEntity existente = new AlunoEntity("Carlos");
        when(alunoRepository.findByNome("Carlos")).thenReturn(Optional.of(existente));

        // When
        AlunoEntity resultado = alunoService.buscarPorNome("Carlos");

        // Then
        assertSame(existente, resultado);
    }

    @Test
    public void deveLancarExcecaoQuandoBuscarPorNomeInexistente() {
        // Given
        when(alunoRepository.findByNome("Fantasma")).thenReturn(Optional.empty());

        // When / Then
        assertThrows(EntityNotFoundException.class, () -> alunoService.buscarPorNome("Fantasma"));
    }

    @Test
    public void deveListarTodosOsAlunosDoRepositorio() {
        // Given
        List<AlunoEntity> alunos = List.of(new AlunoEntity("Duda"), new AlunoEntity("Elias"));
        when(alunoRepository.findAll()).thenReturn(alunos);

        // When
        List<AlunoEntity> resultado = alunoService.listarTodos();

        // Then
        assertEquals(2, resultado.size());
    }

    @Test
    public void deveTraduzirEntidadeParaResponseComTodosOsCampos() {
        // Given
        AlunoEntity entity = new AlunoEntity("Fabio");
        entity.setCursosConcluidos(4);
        entity.setMoedas(3);
        entity.setPlano(Aluno.Plano.PREMIUM);

        // When
        AlunoResponse response = alunoService.paraResponse(entity);

        // Then
        assertEquals("Fabio", response.nome());
        assertEquals(4, response.cursosConcluidos());
        assertEquals(3, response.moedas());
        assertEquals("PREMIUM", response.plano());
    }

    // ---- Desafio 2: regra de Premium agora vive no domínio ----

    @Test
    public void deveReidratarAlunoComEstadoPersistidoSemAplicarRegras() {
        Aluno aluno = new Aluno("Sara", 12, 3, Aluno.Plano.PREMIUM);

        assertEquals(12, aluno.getCursosConcluidos());
        assertEquals(3, aluno.getMoedas());
        assertEquals(Aluno.Plano.PREMIUM, aluno.getPlano());
    }

    @Test
    public void deveVirarPremiumEGanharTresMoedasAoAtingirDozeCursos() {
        Aluno aluno = new Aluno("Tito", 11, 0, Aluno.Plano.BASICO);

        aluno.concluirCurso(new Curso("K8s"), 6.0);

        assertEquals(12, aluno.getCursosConcluidos());
        assertEquals(Aluno.Plano.PREMIUM, aluno.getPlano());
        assertEquals(3, aluno.getMoedas());
    }

    @Test
    public void naoDeveDarMoedasDeNovoNemRebaixarQuandoJaEhPremium() {
        Aluno aluno = new Aluno("Ugo", 12, 3, Aluno.Plano.PREMIUM);

        aluno.concluirCurso(new Curso("Terraform"), 5.0);

        assertEquals(3, aluno.getMoedas());
        assertEquals(Aluno.Plano.PREMIUM, aluno.getPlano());
    }

    @Test
    public void naoDeveVirarPremiumAbaixoDeDozeCursos() {
        Aluno aluno = new Aluno("Vera", 10, 0, Aluno.Plano.BASICO);

        aluno.concluirCurso(new Curso("Git"), 5.0);

        assertEquals(Aluno.Plano.BASICO, aluno.getPlano());
        assertEquals(0, aluno.getMoedas());
    }

    // ---- Porta AlunoPort implementada pelo AlunoService ----

    @Test
    public void deveObterOuCriarAlunoComoObjetoDeDominio() {
        AlunoEntity entity = new AlunoEntity("Wanda");
        entity.setCursosConcluidos(5);
        when(alunoRepository.findByNome("Wanda")).thenReturn(Optional.of(entity));

        Aluno aluno = alunoService.obterOuCriar("Wanda");

        assertEquals("Wanda", aluno.getNome());
        assertEquals(5, aluno.getCursosConcluidos());
    }

    @Test
    public void deveObterAlunoExistenteComoDominioEFalharSeNaoExistir() {
        when(alunoRepository.findByNome("Xico")).thenReturn(Optional.of(new AlunoEntity("Xico")));
        when(alunoRepository.findByNome("Fantasma")).thenReturn(Optional.empty());

        assertEquals("Xico", alunoService.obter("Xico").getNome());
        assertThrows(RecursoNaoEncontradoException.class, () -> alunoService.obter("Fantasma"));
    }

    @Test
    public void deveSalvarEstadoDoDominioNaEntidadeExistente() {
        AlunoEntity existente = new AlunoEntity("Yara");
        when(alunoRepository.findByNome("Yara")).thenReturn(Optional.of(existente));
        when(alunoRepository.save(any(AlunoEntity.class))).thenAnswer(i -> i.getArgument(0));

        AlunoResponse response = alunoService.salvar(new Aluno("Yara", 12, 3, Aluno.Plano.PREMIUM));

        assertEquals(12, existente.getCursosConcluidos());
        assertEquals(3, response.moedas());
        assertEquals("PREMIUM", response.plano());
    }

    @Test
    public void deveCriarEntidadeAoSalvarAlunoAindaInexistente() {
        when(alunoRepository.findByNome("Zeca")).thenReturn(Optional.empty());
        when(alunoRepository.save(any(AlunoEntity.class))).thenAnswer(i -> i.getArgument(0));

        AlunoResponse response = alunoService.salvar(new Aluno("Zeca", 2, 0, Aluno.Plano.BASICO));

        assertEquals("Zeca", response.nome());
        assertEquals(2, response.cursosConcluidos());
    }
}
