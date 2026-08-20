package estevezalvarez.GestarAfeto.alerta.client.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Corpo enviado ao microsservico de alertas para reavaliar o checklist de uma gestante.
 */
public record AvaliarChecklistRequest(
    Long gestanteId,
    String gestanteNome,
    LocalDate dataUltimaMenstruacao,
    LocalDate dataProvavelParto,
    List<ItemChecklistSnapshot> itens
) {}
