package br.com.valueprojects.gamification;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.contribuicao.domain.Contribuicao;
import br.com.valueprojects.gamification.contribuicao.domain.MesDeContribuicoes;
import br.com.valueprojects.gamification.contribuicao.domain.PosicaoRanking;
import br.com.valueprojects.gamification.contribuicao.domain.Sorteador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

// Testes de domínio puro do agregado MesDeContribuicoes: sem Spring, sem banco, sem mocks.
public class ContribuicaoTest {

    // Cenário 1: Dado o aluno com mais contribuições, quando encerra o mês,
    // então o Aluno ganha um curso.
    @Test
    public void deveApurarAlunoComMaisContribuicoesComoVencedorDoMes() {
        // Given
        MesDeContribuicoes mes = new MesDeContribuicoes("Setembro/2026");
        Aluno alunoA = new Aluno("Helena");
        Aluno alunoB = new Aluno("Igor");

        // When (Helena contribui duas vezes, Igor apenas uma)
        mes.registrar(new Contribuicao(alunoA, 1));
        mes.registrar(new Contribuicao(alunoB, 1));
        mes.registrar(new Contribuicao(alunoA, 1));

        // Then
        assertEquals(alunoA, mes.apurarVencedor());
    }

    // Cenário 3: Dado um empate em contribuições de alunos, quando o mês
    // encerra, então o aluno escolhido do mês será sorteado.
    @Test
    public void deveSortearAlunoVencedorEmCasoDeEmpateDeContribuicoes() {
        // Given
        MesDeContribuicoes mes = new MesDeContribuicoes("Setembro/2026");
        Aluno alunoA = new Aluno("Julia");
        Aluno alunoB = new Aluno("Kevin");
        mes.registrar(new Contribuicao(alunoA, 1));
        mes.registrar(new Contribuicao(alunoB, 1));

        // When
        Aluno vencedor = mes.apurarVencedor();

        // Then (sorteio deve escolher um dos dois empatados)
        assertTrue(vencedor.equals(alunoA) || vencedor.equals(alunoB));
    }

    // Cenário 1: ao encerrar o mês, o aluno vencedor deve ganhar um curso.
    @Test
    public void deveConcederCursoAoAlunoVencedorAoEncerrarOMes() {
        // Given
        MesDeContribuicoes mes = new MesDeContribuicoes("Setembro/2026");
        Aluno alunoA = new Aluno("Larissa");
        Aluno alunoB = new Aluno("Marcos");
        mes.registrar(new Contribuicao(alunoA, 1));
        mes.registrar(new Contribuicao(alunoB, 1));
        mes.registrar(new Contribuicao(alunoA, 1));
        int cursosAntes = alunoA.getCursosConcluidos();

        // When
        Aluno vencedor = mes.encerrar();

        // Then
        assertEquals(alunoA, vencedor);
        assertEquals(cursosAntes + 1, alunoA.getCursosConcluidos());
    }

    // Cenário 4: Dado um jogo acontecendo e um Participante válido, quando
    // notar dois ou mais resultados deste participante, então o segundo
    // resultado deve ser ignorado (registro consecutivo do mesmo aluno).
    @Test
    public void deveIgnorarSegundaContribuicaoConsecutivaDoMesmoAluno() {
        // Given
        MesDeContribuicoes mes = new MesDeContribuicoes("Setembro/2026");
        Aluno alunoA = new Aluno("Nara");

        // When (Nara contribui duas vezes seguidas)
        boolean primeira = mes.registrar(new Contribuicao(alunoA, 1));
        boolean segunda = mes.registrar(new Contribuicao(alunoA, 1));

        // Then (a segunda contribuição consecutiva é ignorada)
        assertTrue(primeira);
        assertFalse(segunda);
        assertEquals(1, mes.getContribuicoes().size());
        assertEquals(alunoA, mes.getContribuicoes().get(0).getAluno());
        assertEquals(1, mes.getContribuicoes().get(0).getPeso());
    }

    // BLUE - cobertura: mês sem nenhuma contribuição não tem vencedor e o
    // encerramento não deve conceder curso a ninguém.
    @Test
    public void naoDeveHaverVencedorNemGanhoDeCursoQuandoMesNaoTemContribuicoes() {
        // Given
        MesDeContribuicoes mes = new MesDeContribuicoes("Setembro/2026");

        // When / Then
        assertNull(mes.apurarVencedor());
        assertNull(mes.encerrar());
        assertTrue(mes.isEncerrado());
        assertTrue(mes.ranking().isEmpty());
    }

    // ---- Desafio 2: ranking por SOMA DE PESOS (defeito corrigido) ----

    // Given contribuições com pesos diferentes, When consulta o ranking,
    // Then os pontos são a soma dos pesos por aluno, do maior para o menor.
    @Test
    public void deveSomarPesosPorAlunoEOrdenarRankingDeFormaDecrescente() {
        // Given
        MesDeContribuicoes mes = new MesDeContribuicoes("Outubro/2026");
        Aluno ana = new Aluno("Ana");
        Aluno bia = new Aluno("Bia");
        Aluno caio = new Aluno("Caio");

        // When
        mes.registrar(new Contribuicao(bia, 1));
        mes.registrar(new Contribuicao(ana, 2));
        mes.registrar(new Contribuicao(bia, 1));
        mes.registrar(new Contribuicao(ana, 3));
        mes.registrar(new Contribuicao(caio, 9));
        List<PosicaoRanking> ranking = mes.ranking();

        // Then (Caio 9, Ana 5, Bia 2)
        assertEquals(List.of("Caio", "Ana", "Bia"), ranking.stream().map(p -> p.aluno().getNome()).toList());
        assertEquals(List.of(9.0, 5.0, 2.0), ranking.stream().map(PosicaoRanking::pontos).toList());
    }

    // O critério deixou de ser "quantidade": quem pontua mais vence, mesmo com menos contribuições.
    @Test
    public void deveApurarVencedorPelaSomaDosPesosEMesmoComMenosContribuicoes() {
        // Given (Ana contribui 2x com peso 1; Bia 1x com peso 10)
        MesDeContribuicoes mes = new MesDeContribuicoes("Outubro/2026");
        Aluno ana = new Aluno("Ana");
        Aluno bia = new Aluno("Bia");
        mes.registrar(new Contribuicao(ana, 1));
        mes.registrar(new Contribuicao(bia, 10));
        mes.registrar(new Contribuicao(ana, 1));

        // When / Then
        assertEquals(bia, mes.apurarVencedor());
    }

    // Empate é por soma (0,1 + 0,2 == 0,3), e o desempate usa o sorteador injetado.
    @Test
    public void deveEmpatarPorSomaComPrecisaoDecimalEUsarOSorteadorInjetado() {
        // Given
        AtomicInteger limiteRecebido = new AtomicInteger();
        Sorteador escolheSegundo = limite -> {
            limiteRecebido.set(limite);
            return 1;
        };
        MesDeContribuicoes mes = new MesDeContribuicoes("Outubro/2026", escolheSegundo);
        mes.registrar(new Contribuicao(new Aluno("Ana"), 0.1));
        mes.registrar(new Contribuicao(new Aluno("Bia"), 0.3));
        mes.registrar(new Contribuicao(new Aluno("Ana"), 0.2));

        // When
        Aluno vencedor = mes.apurarVencedor();

        // Then (Ana 0.1+0.2 empata com Bia 0.3; sorteador "escolhe o índice 1")
        assertEquals(2, limiteRecebido.get());
        assertEquals(new Aluno("Bia"), vencedor);
    }

    @Test
    public void deveEscolherPrimeiroEmpatadoQuandoSorteadorSorteiaZero() {
        MesDeContribuicoes mes = new MesDeContribuicoes("Outubro/2026", limite -> 0);
        mes.registrar(new Contribuicao(new Aluno("Ana"), 1));
        mes.registrar(new Contribuicao(new Aluno("Bia"), 1));

        assertEquals(new Aluno("Ana"), mes.apurarVencedor());
    }

    // O sorteador padrão fica sempre dentro do intervalo pedido.
    @Test
    public void sorteadorPadraoDeveRespeitarOLimite() {
        Sorteador sorteador = Sorteador.aleatorio();
        for (int i = 0; i < 50; i++) {
            int sorteado = sorteador.sortear(3);
            assertTrue(sorteado >= 0 && sorteado < 3);
        }
    }

    // ---- Mês encerrado: imutável e idempotente (regra agora no domínio) ----

    @Test
    public void naoDeveAceitarContribuicaoAposEncerrarOMes() {
        // Given
        MesDeContribuicoes mes = new MesDeContribuicoes("Novembro/2026");
        mes.registrar(new Contribuicao(new Aluno("Ana"), 1));
        mes.encerrar();

        // When
        boolean aceita = mes.registrar(new Contribuicao(new Aluno("Bia"), 5));

        // Then
        assertFalse(aceita);
        assertEquals(1, mes.getContribuicoes().size());
    }

    @Test
    public void deveEncerrarUmaUnicaVezSemConcederCursoNemSortearDeNovo() {
        // Given
        MesDeContribuicoes mes = new MesDeContribuicoes("Novembro/2026");
        Aluno ana = new Aluno("Ana");
        mes.registrar(new Contribuicao(ana, 1));

        // When
        Aluno primeira = mes.encerrar();
        Aluno segunda = mes.encerrar();

        // Then
        assertSame(primeira, segunda);
        assertEquals(1, ana.getCursosConcluidos());
        assertEquals(ana, mes.getVencedor());
    }

    // ---- Reidratação: não reavalia o histórico com a regra vigente ----

    @Test
    public void restaurarNaoDeveReaplicarARegraDeContribuicaoConsecutiva() {
        // Given (histórico já gravado com duas contribuições seguidas do mesmo aluno)
        Aluno ana = new Aluno("Ana");
        List<Contribuicao> historico = List.of(new Contribuicao(ana, 1), new Contribuicao(ana, 1));

        // When
        MesDeContribuicoes mes = MesDeContribuicoes.restaurar("Dezembro/2026", historico, false, null,
                Sorteador.aleatorio());

        // Then (registrar() as descartaria; restaurar() preserva o que foi persistido)
        assertEquals(2, mes.getContribuicoes().size());
        assertEquals("Dezembro/2026", mes.getDescricao());
        assertEquals(2.0, mes.ranking().get(0).pontos());
    }

    @Test
    public void mesRestauradoComoEncerradoNaoDeveConcederCursoDeNovo() {
        // Given
        Aluno ana = new Aluno("Ana", 1, 0, Aluno.Plano.BASICO);
        MesDeContribuicoes mes = MesDeContribuicoes.restaurar("Dezembro/2026",
                List.of(new Contribuicao(ana, 1)), true, ana, Sorteador.aleatorio());

        // When
        Aluno vencedor = mes.encerrar();

        // Then
        assertSame(ana, vencedor);
        assertEquals(1, ana.getCursosConcluidos());
    }
}
