package br.com.valueprojects.gamification.curso.service;

import br.com.valueprojects.gamification.aluno.vo.Curso;

/** Porta de saída do catálogo de cursos: o caso de uso não sabe como/onde eles são guardados. */
public interface CursoPort {

    /** Devolve o curso, registrando-o no catálogo se ainda não existir. */
    Curso buscarOuRegistrar(String nome);
}
