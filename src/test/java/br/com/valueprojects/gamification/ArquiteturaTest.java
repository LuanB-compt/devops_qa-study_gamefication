package br.com.valueprojects.gamification;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;

/**
 * Fitness functions do ASR-1 (Manutenibilidade): transformam "disciplina" em
 * regra executável. Rodam em {@code mvn test}/{@code verify}, então um
 * acoplamento indevido derruba o build do Jenkins (estágio de arquitetura).
 *
 * <p>Só analisa código de produção ({@code DoNotIncludeTests}).
 */
@AnalyzeClasses(packages = "br.com.valueprojects.gamification", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArquiteturaTest {

    private static final String BASE = "br.com.valueprojects.gamification.";

    /**
     * DÍVIDA CONHECIDA (aceita nesta decisão, "manter a arquitetura como está"):
     * {@code AlunoService} devolve {@code AlunoEntity}, e curso/assinatura/contribuicao
     * a usam diretamente (e por associação JPA). Qualquer OUTRA entidade ou repositório
     * cruzando módulos reprova o build. Para quitar: AlunoService passar a devolver
     * o domínio/ID, e esta lista ficar vazia.
     */
    private static final Set<String> DIVIDA_CONHECIDA = Set.of(BASE + "aluno.entity.AlunoEntity");

    // 1) Módulos formam um grafo sem ciclos: dá para entender/evoluir um sem os demais.
    @ArchTest
    static final ArchRule modulos_nao_tem_ciclos = slices()
            .matching(BASE + "(*)..")
            .should().beFreeOfCycles();

    // 2) Domínio é Java puro (por isso 100% testável sem Spring/banco).
    @ArchTest
    static final ArchRule dominio_nao_conhece_framework_nem_camadas_tecnicas = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..",
                    "..entity..", "..repository..", "..service..", "..controller..", "..dto..");

    // 3) Entity e Repository são privados do módulo dono: outro módulo só chega aos
    //    dados pelo service/API do dono — assim uma regra nova muda apenas um módulo.
    @ArchTest
    static final ArchRule entity_e_repository_sao_privados_do_modulo = classes()
            .should(naoAcessarEntityOuRepositoryDeOutroModulo());

    // Atenção: a condição já registra a VIOLAÇÃO; por isso é usada com classes() e não
    // noClasses() (a dupla negação deixaria a regra vácua — foi pego por teste de mutação).
    private static ArchCondition<JavaClass> naoAcessarEntityOuRepositoryDeOutroModulo() {
        return new ArchCondition<>("não acessar Entity ou Repository de outro módulo") {
            @Override
            public void check(JavaClass origem, ConditionEvents eventos) {
                for (Dependency dep : origem.getDirectDependenciesFromSelf()) {
                    JavaClass alvo = dep.getTargetClass();
                    boolean alvoEhPersistencia = alvo.getPackageName().endsWith(".entity")
                            || alvo.getPackageName().endsWith(".repository");
                    boolean outroModulo = modulo(alvo) != null && modulo(origem) != null
                            && !modulo(alvo).equals(modulo(origem));
                    if (alvoEhPersistencia && outroModulo && !DIVIDA_CONHECIDA.contains(alvo.getName())) {
                        eventos.add(SimpleConditionEvent.violated(dep, dep.getDescription()));
                    }
                }
            }
        };
    }

    /** "br...gamification.curso.service.X" -> "curso"; classes fora dos módulos -> null. */
    private static String modulo(JavaClass classe) {
        String nome = classe.getPackageName();
        if (!nome.startsWith(BASE)) {
            return null;
        }
        String resto = nome.substring(BASE.length());
        int ponto = resto.indexOf('.');
        return ponto < 0 ? resto : resto.substring(0, ponto);
    }
}
