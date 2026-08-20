package estevezalvarez.gestarafeto.alertas.exception;

import java.time.LocalDateTime;

/**
 * Mesmo formato de erro usado pelo servico principal, para que o front-end trate
 * respostas dos dois servicos de forma uniforme.
 */
public record ErroResponse(
    LocalDateTime timestamp,
    int status,
    String erro,
    String mensagem,
    String path
) {}
