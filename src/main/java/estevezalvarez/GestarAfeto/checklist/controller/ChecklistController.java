package estevezalvarez.GestarAfeto.checklist.controller;

import estevezalvarez.GestarAfeto.checklist.domain.StatusChecklist;
import estevezalvarez.GestarAfeto.checklist.dto.AtualizarStatusChecklistRequest;
import estevezalvarez.GestarAfeto.checklist.dto.ItemChecklistResponse;
import estevezalvarez.GestarAfeto.checklist.service.ChecklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;

    @PostMapping("/api/gestantes/{gestanteId}/checklist/gerar")
    public ResponseEntity<List<ItemChecklistResponse>> gerar(@PathVariable Long gestanteId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistService.gerarChecklist(gestanteId));
    }

    @GetMapping("/api/gestantes/{gestanteId}/checklist")
    public ResponseEntity<List<ItemChecklistResponse>> listar(
            @PathVariable Long gestanteId,
            @RequestParam(required = false) StatusChecklist status) {
        return ResponseEntity.ok(checklistService.listarPorGestante(gestanteId, status));
    }

    @PatchMapping("/api/checklist/{itemId}/status")
    public ResponseEntity<ItemChecklistResponse> atualizarStatus(
            @PathVariable Long itemId,
            @Valid @RequestBody AtualizarStatusChecklistRequest request) {
        return ResponseEntity.ok(checklistService.atualizarStatus(itemId, request));
    }

    @PatchMapping("/api/checklist/{itemId}/realizar")
    public ResponseEntity<ItemChecklistResponse> marcarRealizado(@PathVariable Long itemId) {
        return ResponseEntity.ok(checklistService.marcarRealizado(itemId));
    }

    @PatchMapping("/api/checklist/{itemId}/revisar")
    public ResponseEntity<ItemChecklistResponse> marcarParaRevisar(@PathVariable Long itemId) {
        return ResponseEntity.ok(checklistService.marcarParaRevisar(itemId));
    }
}
