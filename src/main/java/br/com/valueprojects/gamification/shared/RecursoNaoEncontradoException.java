package br.com.valueprojects.gamification.shared;

/**
 * Erro de negócio "recurso não encontrado", neutro em relação à tecnologia.
 *
 * <p>Existe para que os application services (casos de uso) não precisem
 * importar {@code jakarta.persistence.EntityNotFoundException}: usar a exceção
 * do JPA como contrato de erro acoplaria o caso de uso ao mecanismo de
 * persistência, justamente o que a decisão do Desafio 2 quer evitar.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
