package br.com.valueprojects.gamification.aluno.service;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;

/**
 * Porta de acesso ao aluno para os outros módulos (quitação PARCIAL da dívida
 * da {@code AlunoEntity}).
 *
 * <p>curso, assinatura e contribuicao deixam de receber {@code AlunoEntity} nos
 * seus services: pedem e devolvem o {@link Aluno} de domínio. Escrever o estado
 * do aluno passa a ser uma operação explícita ({@link #salvar}), em vez de
 * depender do dirty checking do JPA sobre uma entidade "emprestada".
 */
public interface AlunoPort {

    /** Aluno com o estado atual; cria um novo (zerado) se ainda não existir. */
    Aluno obterOuCriar(String nome);

    /** @throws br.com.valueprojects.gamification.shared.RecursoNaoEncontradoException se não existir */
    Aluno obter(String nome);

    /** Grava o estado calculado pelo domínio e devolve a visão de API. */
    AlunoResponse salvar(Aluno aluno);
}
