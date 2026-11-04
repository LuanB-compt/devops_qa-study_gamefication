package br.com.valueprojects.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.valueprojects.gamification.aluno.repository.AlunoRepository;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import br.com.valueprojects.gamification.contribuicao.dto.VencedorResponse;
import br.com.valueprojects.gamification.contribuicao.service.MesDeContribuicoesService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Regressão do bug "encerrar o mês duas vezes concede o curso duas vezes".
 *
 * <p>Usa JPA + H2 reais (e não mocks) porque o defeito estava no <i>estado
 * persistido</i> entre requisições: um mock devolveria o que o teste mandasse e
 * não provaria que o "encerrado" sobrevive ao ciclo gravar/ler do banco. Por
 * isso, entre duas chamadas o teste faz flush + clear do contexto de
 * persistência, forçando a segunda chamada a ler do banco.
 *
 * <p>As propriedades sobrescrevem o dialeto PostgreSQL do perfil dev (o
 * {@code @DataJpaTest} troca o DataSource por um H2 embutido).
 */
@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({ MesDeContribuicoesService.class, AlunoService.class })
public class EncerramentoDoMesIntegrationTest {

    @Autowired
    private MesDeContribuicoesService service;

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private TestEntityManager em;

    private void forcarLeituraDoBanco() {
        em.flush();
        em.clear();
    }

    // Bug original: Given um mês já encerrado, When encerra de novo,
    // Then o vencedor NÃO ganha outro curso.
    @Test
    public void naoDeveConcederCursoNovamenteAoEncerrarMesJaEncerrado() {
        // Given
        service.registrarContribuicao("Outubro/2026", "Ana", 1);
        service.registrarContribuicao("Outubro/2026", "Bruno", 1);
        service.registrarContribuicao("Outubro/2026", "Ana", 1);
        VencedorResponse primeira = service.encerrarMes("Outubro/2026");
        forcarLeituraDoBanco();

        // When
        VencedorResponse segunda = service.encerrarMes("Outubro/2026");
        forcarLeituraDoBanco();

        // Then
        assertEquals("Ana", primeira.alunoVencedor());
        assertEquals(1, primeira.cursosConcluidos());
        assertEquals(primeira, segunda);
        assertEquals(1, alunoRepository.findByNome("Ana").orElseThrow().getCursosConcluidos());
    }

    // Cenário 3 + idempotência: em empate o sorteio acontece uma única vez; a
    // segunda chamada devolve o mesmo sorteado, e só UM aluno ganha curso.
    @Test
    public void naoDeveSortearDeNovoEmEmpateAoEncerrarMesJaEncerrado() {
        // Given (empate: cada aluno com 1 contribuição)
        service.registrarContribuicao("Novembro/2026", "Carla", 1);
        service.registrarContribuicao("Novembro/2026", "Davi", 1);
        VencedorResponse primeira = service.encerrarMes("Novembro/2026");
        forcarLeituraDoBanco();

        // When
        VencedorResponse segunda = service.encerrarMes("Novembro/2026");
        forcarLeituraDoBanco();

        // Then
        assertEquals(primeira.alunoVencedor(), segunda.alunoVencedor());
        int totalDeCursos = alunoRepository.findByNome("Carla").orElseThrow().getCursosConcluidos()
                + alunoRepository.findByNome("Davi").orElseThrow().getCursosConcluidos();
        assertEquals(1, totalDeCursos);
    }

    // Mês sem contribuições também fecha uma vez só, sem erro e sem vencedor.
    @Test
    public void deveEncerrarMesSemContribuicoesDeFormaIdempotente() {
        // Given
        service.buscarOuCriarMes("Dezembro/2026");
        forcarLeituraDoBanco();

        // When
        VencedorResponse primeira = service.encerrarMes("Dezembro/2026");
        forcarLeituraDoBanco();
        VencedorResponse segunda = service.encerrarMes("Dezembro/2026");

        // Then
        assertNull(primeira.alunoVencedor());
        assertEquals(primeira, segunda);
    }

    // Invariante: mês apurado é imutável (senão o vencedor gravado ficaria defasado).
    @Test
    public void naoDeveAceitarContribuicaoEmMesJaEncerrado() {
        // Given
        service.registrarContribuicao("Janeiro/2027", "Elisa", 1);
        service.encerrarMes("Janeiro/2027");
        forcarLeituraDoBanco();

        // When
        boolean registrada = service.registrarContribuicao("Janeiro/2027", "Felipe", 1);
        forcarLeituraDoBanco();

        // Then
        assertFalse(registrada);
        assertEquals(1, service.consultarRanking("Janeiro/2027").contribuicoes().size());
        assertTrue(alunoRepository.findByNome("Felipe").isEmpty(),
                "aluno não deve ser criado por uma contribuição rejeitada");
    }

    @Test
    public void deveFalharAoEncerrarMesInexistente() {
        assertThrows(EntityNotFoundException.class, () -> service.encerrarMes("Mes/Inexistente"));
    }
}
