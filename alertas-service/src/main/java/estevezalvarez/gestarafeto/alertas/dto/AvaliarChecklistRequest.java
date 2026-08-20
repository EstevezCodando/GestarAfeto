package estevezalvarez.gestarafeto.alertas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato de entrada da avaliacao. O servico principal envia o estado atual do checklist
 * e este microsservico devolve os alertas resultantes.
 */
public record AvaliarChecklistRequest(
    @NotNull Long gestanteId,
    @NotBlank String gestanteNome,
    LocalDate dataUltimaMenstruacao,
    LocalDate dataProvavelParto,
    @NotNull @Valid List<ItemChecklistSnapshot> itens
) {}
