package br.com.valueprojects.gamification;

import br.com.valueprojects.gamification.aluno.service.AlunoPort;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import br.com.valueprojects.gamification.contribuicao.repository.MesDeContribuicoesJpaAdapter;
import br.com.valueprojects.gamification.contribuicao.service.MesDeContribuicoesPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Contrato executado contra o adapter JPA REAL (H2). O flush + clear entre as
 * operações força a leitura do banco: prova que o estado sobrevive ao ciclo
 * gravar/ler, que é o que um fake em memória não consegue provar sozinho.
 */
@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({ MesDeContribuicoesJpaAdapter.class, AlunoService.class })
public class MesDeContribuicoesJpaPortTest extends MesDeContribuicoesPortContract {

    @Autowired
    private MesDeContribuicoesJpaAdapter adapter;

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private TestEntityManager em;

    @Override
    protected MesDeContribuicoesPort porta() {
        return adapter;
    }

    @Override
    protected AlunoPort alunos() {
        return alunoService;
    }

    @Override
    protected void forcarLeituraDoBanco() {
        em.flush();
        em.clear();
    }
}
