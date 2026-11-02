package br.com.valueprojects.gamification;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.contribuicao.domain.Contribuicao;
import br.com.valueprojects.gamification.contribuicao.domain.MesDeContribuicoes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// RED - TDD
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
        mes.registrar(new Contribuicao(alunoA, 1));
        mes.registrar(new Contribuicao(alunoA, 1));

        // Then (a segunda contribuição consecutiva é ignorada)
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
    }
}
