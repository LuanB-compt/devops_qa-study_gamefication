package br.com.valueprojects.gamification.fake;

import br.com.valueprojects.gamification.aluno.vo.Curso;
import br.com.valueprojects.gamification.curso.service.CursoPort;
import java.util.LinkedHashSet;
import java.util.Set;

/** Adapter em memória da {@link CursoPort}. */
public class FakeCursoPort implements CursoPort {

    private final Set<String> catalogo = new LinkedHashSet<>();

    @Override
    public Curso buscarOuRegistrar(String nome) {
        catalogo.add(nome);
        return new Curso(nome);
    }

    public Set<String> catalogo() {
        return catalogo;
    }
}
