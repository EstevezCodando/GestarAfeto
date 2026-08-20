package estevezalvarez.GestarAfeto.checklist.dto;

import estevezalvarez.GestarAfeto.checklist.domain.ItemChecklistGestante;
import estevezalvarez.GestarAfeto.checklist.domain.StatusChecklist;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ItemChecklistResponse(
    Long id,
    ProcedimentoResumoResponse procedimento,
    StatusChecklist status,
    LocalDate dataPrevista,
    LocalDate dataRealizacao,
    String observacao,
    LocalDateTime dataCriacao
) {
    public static ItemChecklistResponse from(ItemChecklistGestante item) {
        return new ItemChecklistResponse(
            item.getId(),
            ProcedimentoResumoResponse.from(item.getProcedimento()),
            item.getStatus(),
            item.getDataPrevista(),
            item.getDataRealizacao(),
            item.getObservacao(),
            item.getDataCriacao()
        );
    }
}
