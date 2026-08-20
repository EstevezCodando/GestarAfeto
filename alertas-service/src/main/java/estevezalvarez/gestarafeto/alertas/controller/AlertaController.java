package estevezalvarez.gestarafeto.alertas.controller;

import estevezalvarez.gestarafeto.alertas.domain.PrioridadeAlerta;
import estevezalvarez.gestarafeto.alertas.domain.StatusAlerta;
import estevezalvarez.gestarafeto.alertas.dto.AlertaResponse;
import estevezalvarez.gestarafeto.alertas.dto.AvaliarChecklistRequest;
import estevezalvarez.gestarafeto.alertas.dto.ResumoAlertasResponse;
import estevezalvarez.gestarafeto.alertas.service.AlertaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST do microsservico de alertas.
 *
 * <p>Consumida pelo servico principal (via Feign) e disponivel para chamadas diretas
 * durante testes e demonstracoes.</p>
 */
@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;

    /**
     * Reavalia o checklist de uma gestante e devolve os alertas ativos resultantes.
     * Idempotente: pode ser chamado a cada mudanca de checklist.
     */
    @PostMapping("/avaliacoes")
    public ResponseEntity<List<AlertaResponse>> avaliar(@Valid @RequestBody AvaliarChecklistRequest request) {
        return ResponseEntity.ok(alertaService.avaliar(request));
    }

    @GetMapping
    public ResponseEntity<Page<AlertaResponse>> listar(
            @RequestParam(required = false) Long gestanteId,
            @RequestParam(required = false) StatusAlerta status,
            @RequestParam(required = false) PrioridadeAlerta prioridade,
            Pageable pageable) {
        return ResponseEntity.ok(alertaService.listarPaginado(gestanteId, status, prioridade, pageable));
    }

    @GetMapping("/gestante/{gestanteId}")
    public ResponseEntity<List<AlertaResponse>> listarAtivosPorGestante(@PathVariable Long gestanteId) {
        return ResponseEntity.ok(alertaService.listarAtivos(gestanteId));
    }

    @GetMapping("/gestante/{gestanteId}/resumo")
    public ResponseEntity<ResumoAlertasResponse> resumo(@PathVariable Long gestanteId) {
        return ResponseEntity.ok(alertaService.resumo(gestanteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(alertaService.buscarPorId(id));
    }

    @PatchMapping("/{id}/leitura")
    public ResponseEntity<AlertaResponse> marcarLido(@PathVariable Long id) {
        return ResponseEntity.ok(alertaService.marcarLido(id));
    }

    @PatchMapping("/{id}/resolucao")
    public ResponseEntity<AlertaResponse> resolver(@PathVariable Long id) {
        return ResponseEntity.ok(alertaService.resolver(id));
    }

    @PatchMapping("/{id}/cancelamento")
    public ResponseEntity<AlertaResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(alertaService.cancelar(id));
    }

    /** Usado pelo servico principal quando uma gestante e removida. */
    @DeleteMapping("/gestante/{gestanteId}")
    public ResponseEntity<Void> removerPorGestante(@PathVariable Long gestanteId) {
        alertaService.removerPorGestante(gestanteId);
        return ResponseEntity.noContent().build();
    }
}
