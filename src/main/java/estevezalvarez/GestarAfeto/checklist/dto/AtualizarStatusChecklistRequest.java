package estevezalvarez.GestarAfeto.checklist.dto;

import estevezalvarez.GestarAfeto.checklist.domain.StatusChecklist;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AtualizarStatusChecklistRequest(
    @NotNull(message = "Status é obrigatório") StatusChecklist status,
    LocalDate dataRealizacao,
    String observacao
) {}
