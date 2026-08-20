package estevezalvarez.GestarAfeto.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNotFound(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ErroResponse(LocalDateTime.now(), 404, "Recurso não encontrado", ex.getMessage(), req.getRequestURI())
        );
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> handleBusinessRule(RegraDeNegocioException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            new ErroResponse(LocalDateTime.now(), 422, "Regra de negócio violada", ex.getMessage(), req.getRequestURI())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErroResponse(LocalDateTime.now(), 400, "Dados inválidos", mensagem, req.getRequestURI())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            new ErroResponse(LocalDateTime.now(), 409, "Integridade violada",
                "A operacao viola uma restricao de banco de dados.", req.getRequestURI())
        );
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErroResponse> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            new ErroResponse(LocalDateTime.now(), 409, "Conflito de concorrencia",
                "O registro foi alterado por outra transacao. Recarregue os dados e tente novamente.", req.getRequestURI())
        );
    }

    @ExceptionHandler(ServicoIndisponivelException.class)
    public ResponseEntity<ErroResponse> handleServicoIndisponivel(
            ServicoIndisponivelException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            new ErroResponse(LocalDateTime.now(), 503, "Servico indisponivel", ex.getMessage(),
                req.getRequestURI())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ErroResponse(LocalDateTime.now(), 500, "Erro interno", "Ocorreu um erro inesperado.", req.getRequestURI())
        );
    }
}
