package br.com.valueprojects.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import br.com.valueprojects.gamification.assinatura.domain.Assinatura;
import br.com.valueprojects.gamification.assinatura.dto.AssinaturaResponse;
import br.com.valueprojects.gamification.assinatura.repository.AssinaturaJpaAdapter;
import br.com.valueprojects.gamification.assinatura.service.AssinaturaService;
import br.com.valueprojects.gamification.curso.repository.CursoJpaAdapter;
import br.com.valueprojects.gamification.curso.repository.CursoRepository;
import br.com.valueprojects.gamification.curso.service.CursoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Os adapters JPA de assinatura e curso contra H2 real. O fake prova que o caso
 * de uso não depende do banco; este teste prova que o adapter real cumpre o mesmo
 * papel (inclusive a query derivada {@code findByAlunoNome}).
 */
@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({ AssinaturaService.class, AssinaturaJpaAdapter.class, CursoService.class, CursoJpaAdapter.class,
        AlunoService.class })
public class AdaptersJpaIntegrationTest {

    @Autowired
    private AssinaturaService assinaturaService;
    @Autowired
    private AssinaturaJpaAdapter assinaturaAdapter;
    @Autowired
    private CursoService cursoService;
    @Autowired
    private CursoRepository cursoRepository;
    @Autowired
    private TestEntityManager em;

    private void forcarLeituraDoBanco() {
        em.flush();
        em.clear();
    }

    // Cenário 2 de ponta a ponta sobre o banco: o cancelamento sobrevive ao ciclo gravar/ler.
    @Test
    public void devePersistirCancelamentoDaAssinaturaPorFaltaDePagamento() {
        // Given
        assinaturaService.criar("Ana");
        forcarLeituraDoBanco();

        // When
        assinaturaService.encerrarMes("Ana");
        forcarLeituraDoBanco();

        // Then
        AssinaturaResponse lida = assinaturaService.consultar("Ana");
        assertFalse(lida.ativa());
        assertFalse(lida.pagamentoRealizado());
    }

    @Test
    public void devePersistirPagamentoSemDuplicarAAssinatura() {
        assinaturaService.criar("Bia");
        assinaturaService.registrarPagamento("Bia");
        forcarLeituraDoBanco();

        // criar de novo devolve a existente (não cria uma segunda por causa do unique aluno_id)
        AssinaturaResponse response = assinaturaService.criar("Bia");
        forcarLeituraDoBanco();

        assertTrue(response.pagamentoRealizado());
        assertTrue(assinaturaAdapter.buscarPorAluno("Bia").orElseThrow().isPagamentoRealizado());
    }

    @Test
    public void deveDevolverVazioParaAlunoSemAssinatura() {
        assertTrue(assinaturaAdapter.buscarPorAluno("Ninguem").isEmpty());
    }

    @Test
    public void deveSalvarAssinaturaCriandoOAlunoQuandoNecessario() {
        assinaturaAdapter.salvar(new Assinatura(new Aluno("Caio"), true, false));
        forcarLeituraDoBanco();

        assertTrue(assinaturaAdapter.buscarPorAluno("Caio").orElseThrow().isAtiva());
    }

    // Cenário 5 + regra de Premium (agora no domínio) persistidas via AlunoPort/JPA.
    @Test
    public void devePersistirConclusaoDeCursoComBonusEPromocaoAPremium() {
        // Given (11 cursos: 3 conclusões com bônus = 12 cursos... começamos de 0)
        cursoService.concluirCurso("Duda", "C1", 9.0); // 4
        cursoService.concluirCurso("Duda", "C2", 9.0); // 8
        forcarLeituraDoBanco();

        // When (+4 = 12, fecha o Premium)
        var response = cursoService.concluirCurso("Duda", "C3", 9.0);
        forcarLeituraDoBanco();

        // Then
        assertEquals(12, response.cursosConcluidos());
        assertEquals("PREMIUM", response.plano());
        assertEquals(3, response.moedas());
    }

    @Test
    public void deveRegistrarCursoNovoUmaUnicaVez() {
        cursoService.concluirCurso("Edu", "Docker", 5.0);
        cursoService.concluirCurso("Edu", "Docker", 5.0);
        forcarLeituraDoBanco();

        assertEquals(1, cursoRepository.count());
    }
}
