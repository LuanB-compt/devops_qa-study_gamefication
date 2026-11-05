package br.com.valueprojects.gamification.fake;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.aluno.service.AlunoPort;
import br.com.valueprojects.gamification.shared.RecursoNaoEncontradoException;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter em memória da {@link AlunoPort}. Guarda e devolve CÓPIAS, como um
 * banco faz: alterar o objeto devolvido não muda o "persistido" até chamar
 * {@link #salvar}. Sem isso o fake esconderia o bug de "escrita implícita".
 */
public class FakeAlunoPort implements AlunoPort {

    private final Map<String, Aluno> persistidos = new HashMap<>();

    @Override
    public Aluno obterOuCriar(String nome) {
        return copia(persistidos.computeIfAbsent(nome, Aluno::new));
    }

    @Override
    public Aluno obter(String nome) {
        Aluno aluno = persistidos.get(nome);
        if (aluno == null) {
            throw new RecursoNaoEncontradoException("Aluno não encontrado: " + nome);
        }
        return copia(aluno);
    }

    @Override
    public AlunoResponse salvar(Aluno aluno) {
        persistidos.put(aluno.getNome(), copia(aluno));
        return new AlunoResponse((long) persistidos.size(), aluno.getNome(), aluno.getCursosConcluidos(),
                aluno.getMoedas(), aluno.getPlano().name());
    }

    public boolean existe(String nome) {
        return persistidos.containsKey(nome);
    }

    private static Aluno copia(Aluno a) {
        return new Aluno(a.getNome(), a.getCursosConcluidos(), a.getMoedas(), a.getPlano());
    }
}
