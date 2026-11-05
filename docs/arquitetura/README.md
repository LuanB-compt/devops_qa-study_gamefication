# Registro Arquitetural — B1 Architecture Lab

Registro cumulativo das decisões arquiteturais da plataforma de Gamificação para Educação Continuada.

**Equipe:** Luan Bruno Domingues de Oliveira (RA 210200) e Juan Pedro Jerônimo Guilen (RA 200679).

Cada decisão segue a mesma cadeia: RF → RNF → ASR + RPC → Alternativas → ADR → C4 → Evidência.

## Decisões

| ADR | Desafio | Decisão | C4 | Status |
|---|---|---|---|---|
| [ADR-001](ADR_desafio1.md) | 1 — Arquitetura macro | Monólito Modular com fronteiras verificadas no build e encerramento de mês idempotente | [Container](ADR_desafio1.md#6-evidência-da-resolução-arquitetura-c4-model) | Aceito |
| [ADR-002](ADR_desafio2.md) | 2 — Organização interna do Core | Ports & Adapters seletivo, sem reorganizar pacotes | [Component](ADR_desafio2.md#c4-component-do-core) | Aceito |

## Matriz Cumulativa de ASRs

| ASR | Qualidade | Desafio | O que altera | Como é verificado |
|---|---|---|---|---|
| **ASR-1** | Manutenibilidade | 1 | Estrutura do Core: módulos sem ciclos, dados privados de cada módulo | Regras 1 a 3 do `ArquiteturaTest` |
| **ASR-2** | Modificabilidade | 2 | Estrutura: cada regra em um único lugar, o domínio | Experimento de troca do critério de apuração; regra 2 do `ArquiteturaTest` |
| **ASR-3** | Testabilidade | 2 | Estrutura e dependências: services dependem de portas | Mini-lab com fake e JPA; regras 4 a 6 do `ArquiteturaTest` |

## Evidências

| Desafio | Evidência | Onde |
|---|---|---|
| 1 | Regras de arquitetura no build | `src/test/java/.../ArquiteturaTest.java`; estágio 2a do `Jenkinsfile` |
| 1 | Idempotência do encerramento | `src/test/java/.../EncerramentoDoMesIntegrationTest.java` |
| 2 | Mini-lab: casos de uso sem Spring e sem banco | `src/test/java/.../MesDeContribuicoesServiceTest.java`; `src/test/java/.../fake/` |
| 2 | Mini-lab: mesma suíte no fake e no JPA | `src/test/java/.../MesDeContribuicoesPortContract.java` |
| 2 | Resultados medidos | [ADR-002, seção 6](ADR_desafio2.md#resultados-medidos) |

Para reproduzir: `mvn clean verify`. O relatório de cobertura fica em `target/site/jacoco/index.html`.
