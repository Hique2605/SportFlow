package com.example.SportFlow.Exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Tratamento centralizado de erros. Toda resposta de erro sai no formato
 * ProblemDetail (RFC 9457): { "status", "title", "detail", "instance" }.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Registro não encontrado
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail naoEncontrado(EntityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 400 - Falha nas anotações de validação (@NotBlank, @Email, @Size...) dos DTOs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail dadosInvalidos(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Dados inválidos");

        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, mensagem);
    }

    // 400 - Validação de parâmetros de método
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail parametrosInvalidos(HandlerMethodValidationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Parâmetros inválidos");
    }

    // 400 - JSON inválido
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail jsonInvalido(HttpMessageNotReadableException ex) {
        // Mostra o motivo (ex.: campo de hora com texto inválido) para facilitar o teste
        String motivo = ex.getMostSpecificCause().getMessage();
        if (motivo != null) {
            motivo = motivo.split("\\R")[0];
            if (motivo.length() > 200) motivo = motivo.substring(0, 200) + "...";
        }
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "JSON inválido ou campos com tipos incompatíveis. Horas devem ser \"HH:mm:ss\" (ex.: \"08:00:00\"), datas \"aaaa-mm-dd\" e ids devem ser UUIDs existentes."
                        + (motivo != null ? " Detalhe: " + motivo : "")
        );
    }

    // 400 - Parâmetro da URL com tipo errado (ex.: id que não é UUID)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Parâmetro '" + ex.getName() + "' com valor inválido: " + ex.getValue()
        );
    }

    // 401 - Login com usuário inativo
    @ExceptionHandler(DisabledException.class)
    public ProblemDetail usuarioInativo(DisabledException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Usuário inativo");
    }

    // 401 - E-mail ou senha incorretos no login
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail naoAutenticado(AuthenticationException ex) {
        log.warn("Falha de autenticação: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
    }

    // 403 - Logado, mas sem permissão (ex.: CLIENTE acessando recurso de outro usuário)
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail acessoNegado(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
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
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Demais erros: ResponseStatusException (lançada nos services), 404 de rota,
    // 405 de método etc. já carregam status e mensagem. O resto vira 500.
    @ExceptionHandler(Exception.class)
    public ProblemDetail erroGeral(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            return errorResponse.getBody();
        }
        log.error("Erro inesperado", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno no servidor."
        );
    }
}
