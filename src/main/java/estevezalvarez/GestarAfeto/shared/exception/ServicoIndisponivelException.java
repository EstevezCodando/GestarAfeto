package estevezalvarez.GestarAfeto.shared.exception;

/**
 * Um servico externo necessario para concluir a operacao nao respondeu.
 * Mapeada para 503 pelo {@link GlobalExceptionHandler}.
 */
public class ServicoIndisponivelException extends RuntimeException {
    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
