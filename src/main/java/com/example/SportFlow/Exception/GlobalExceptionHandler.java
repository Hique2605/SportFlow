package com.example.SportFlow.Exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Registro não encontrado
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail naoEncontrado(EntityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }

    // 400 - Dados inválidos
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail dadosInvalidos(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Dados inválidos");

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                mensagem
        );
    }

    // 400 - JSON inválido
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail jsonInvalido(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "JSON inválido ou campos com tipos incompatíveis."
        );
    }

    // 409 - Conflito de dados
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflitoDados(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Operação não permitida devido a uma restrição de integridade dos dados."
        );
    }

    // 409 - Regra de negócio
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail regraNegocio(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }
}