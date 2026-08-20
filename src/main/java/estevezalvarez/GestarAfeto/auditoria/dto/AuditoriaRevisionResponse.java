package estevezalvarez.GestarAfeto.auditoria.dto;

import java.time.Instant;
import java.util.Map;

public record AuditoriaRevisionResponse(
    Integer revisao,
    Instant dataRevisao,
    String tipoAlteracao,
    Map<String, Object> dados
) {
}
