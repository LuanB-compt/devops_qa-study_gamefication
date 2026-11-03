package br.com.valueprojects.gamification;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import br.com.valueprojects.gamification.curso.entity.CursoEntity;
import br.com.valueprojects.gamification.curso.repository.CursoRepository;
import br.com.valueprojects.gamification.curso.service.CursoService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Testes unitários (Mockito) da camada service - CursoService reaproveita
// Aluno#concluirCurso(Curso, double) do domínio (cenário 5) para decidir o
// delta de cursos; aqui validamos a orquestração com os repositórios e a
// regra extra do case (12 cursos -> plano Premium + 3 moedas).
@ExtendWith(MockitoExtension.class)
public class CursoTest {

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private AlunoService alunoService;

    @InjectMocks
    private CursoService cursoService;

    @Test
    public void deveConcluirCursoExistenteSemBonusQuandoMediaNaoUltrapassaSete() {
        // Given
        AlunoEntity aluno = new AlunoEntity("Gustavo");
        CursoEntity curso = new CursoEntity("Java Básico");
        when(alunoService.buscarOuCriar("Gustavo")).thenReturn(aluno);
        when(cursoRepository.findByNome("Java Básico")).thenReturn(Optional.of(curso));
        when(alunoService.paraResponse(aluno))
                .thenReturn(new AlunoResponse(1L, "Gustavo", aluno.getCursosConcluidos(), 0, "BASICO"));

        // When
        AlunoResponse response = cursoService.concluirCurso("Gustavo", "Java Básico", 7.0);

        // Then (só o curso concluído, sem bônus)
        assertEquals(1, aluno.getCursosConcluidos());
        assertEquals("BASICO", response.plano());
        verify(cursoRepository, never()).save(any());
    }

    @Test
    public void deveCriarCursoNovoEConcederBonusQuandoMediaAcimaDeSete() {
        // Given
        AlunoEntity aluno = new AlunoEntity("Helena");
        when(alunoService.buscarOuCriar("Helena")).thenReturn(aluno);
        when(cursoRepository.findByNome("Docker")).thenReturn(Optional.empty());
        when(cursoRepository.save(any(CursoEntity.class))).thenReturn(new CursoEntity("Docker"));
        when(alunoService.paraResponse(aluno))
                .thenReturn(new AlunoResponse(2L, "Helena", aluno.getCursosConcluidos(), 0, "BASICO"));

        // When
        cursoService.concluirCurso("Helena", "Docker", 9.0);

        // Then (1 do curso + 3 de bônus pela média > 7)
        assertEquals(4, aluno.getCursosConcluidos());
        verify(cursoRepository).save(any(CursoEntity.class));
    }

    @Test
    public void deveVirarPremiumEGanharMoedasAoAtingirDozeCursos() {
        // Given: aluno faltando 1 curso para os 12 necessários ao plano Premium
        AlunoEntity aluno = new AlunoEntity("Igor");
        aluno.setCursosConcluidos(11);
        when(alunoService.buscarOuCriar("Igor")).thenReturn(aluno);
        when(cursoRepository.findByNome("Kubernetes")).thenReturn(Optional.of(new CursoEntity("Kubernetes")));
        when(alunoService.paraResponse(aluno))
                .thenReturn(new AlunoResponse(3L, "Igor", aluno.getCursosConcluidos(), aluno.getMoedas(), "PREMIUM"));

        // When (média <= 7, então só +1 curso, mas isso já fecha os 12)
        cursoService.concluirCurso("Igor", "Kubernetes", 6.0);

        // Then
        assertEquals(12, aluno.getCursosConcluidos());
        assertEquals(Aluno.Plano.PREMIUM, aluno.getPlano());
        assertEquals(3, aluno.getMoedas());
    }

    @Test
    public void naoDeveAlterarPlanoQuandoJaEhPremium() {
        // Given: aluno já Premium, para cobrir o branch que evita processar de novo
        AlunoEntity aluno = new AlunoEntity("Julia");
        aluno.setCursosConcluidos(12);
        aluno.setPlano(Aluno.Plano.PREMIUM);
        aluno.setMoedas(3);
        when(alunoService.buscarOuCriar("Julia")).thenReturn(aluno);
        when(cursoRepository.findByNome("Terraform")).thenReturn(Optional.of(new CursoEntity("Terraform")));
        when(alunoService.paraResponse(aluno))
                .thenReturn(new AlunoResponse(4L, "Julia", aluno.getCursosConcluidos(), aluno.getMoedas(), "PREMIUM"));

        // When
        cursoService.concluirCurso("Julia", "Terraform", 5.0);

        // Then (moedas não dobram, plano continua Premium)
        assertEquals(3, aluno.getMoedas());
        assertEquals(Aluno.Plano.PREMIUM, aluno.getPlano());
    }
}
