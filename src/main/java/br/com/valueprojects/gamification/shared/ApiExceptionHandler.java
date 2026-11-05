package br.com.valueprojects.gamification.shared;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    /**
    * Traduz exceções lançadas pelos services (ex.: aluno/mês/assinatura não
    * encontrado) em respostas HTTP apropriadas, para todos os controllers da API.
    *
    * <p>Trata as duas formas: a exceção neutra dos casos de uso e a do JPA, que
    * ainda é lançada pelo módulo {@code aluno} (fora do escopo desta decisão).
    */
    @ExceptionHandler({ RecursoNaoEncontradoException.class, EntityNotFoundException.class })
    public ResponseEntity<String> tratarNaoEncontrado(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
