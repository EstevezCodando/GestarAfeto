package estevezalvarez.gestarafeto.alertas.mensageria.evento;

import java.time.Instant;

/** Evento recebido apos a remocao de uma gestante no servico principal. */
public record GestanteRemovidaMessage(
    String eventoId,
    String tipo,
    int versao,
    Instant ocorridoEm,
    String origem,
    Long gestanteId
) {
}
