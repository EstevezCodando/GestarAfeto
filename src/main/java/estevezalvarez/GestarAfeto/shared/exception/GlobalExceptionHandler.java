package estevezalvarez.GestarAfeto.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    /**
     * Erros de protocolo (caminho inexistente, metodo nao suportado, parametro ausente...). Sao
     * falhas do cliente e carregam o proprio status; sem este handler o catch-all abaixo as
     * transformaria em 500, o que esconde o erro de quem chamou e polui o alarme de falhas reais.
     */
    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class,
        HttpMediaTypeNotSupportedException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErroResponse> handleProtocolo(Exception ex, HttpServletRequest req) {
        HttpStatusCode status = ex instanceof org.springframework.web.ErrorResponse resposta
            ? resposta.getStatusCode() : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(
            new ErroResponse(LocalDateTime.now(), status.value(), "Requisicao invalida",
                mensagemPara(status), req.getRequestURI())
        );
    }

    /** Corpo JSON malformado ou parametro de rota/consulta de tipo errado (ex.: id=abc). */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroResponse> handleRequisicaoMalFormada(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErroResponse(LocalDateTime.now(), 400, "Requisicao invalida",
                "O corpo ou os parametros da requisicao estao mal formados.", req.getRequestURI())
        );
    }

    private static String mensagemPara(HttpStatusCode status) {
        if (status.value() == 404) return "Recurso nao encontrado.";
        if (status.value() == 405) return "Metodo HTTP nao suportado para este recurso.";
        if (status.value() == 415) return "Tipo de conteudo nao suportado.";
        return "A requisicao nao pode ser atendida.";
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        // Sem este log o erro 500 desapareceria: a resposta nao traz a causa, e o log e o unico lugar onde ela fica.
        log.error("Erro inesperado em {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ErroResponse(LocalDateTime.now(), 500, "Erro interno", "Ocorreu um erro inesperado.", req.getRequestURI())
        );
    }
}
