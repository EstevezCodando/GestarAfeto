package estevezalvarez.GestarAfeto.alerta.controller;

import estevezalvarez.GestarAfeto.alerta.client.dto.AlertaResponse;
import estevezalvarez.GestarAfeto.alerta.client.dto.ResumoAlertasResponse;
import estevezalvarez.GestarAfeto.alerta.service.AlertaIntegracaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints REST de alertas expostos pelo servico principal.
 *
 * <p>O front-end fala apenas com esta API: o servico principal atua como fachada para o
 * microsservico. Isso mantem uma unica origem HTTP para o navegador, concentra o tratamento
 * de erro e permite validar a existencia da gestante antes de chamar o microsservico.</p>
 */
@RestController
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaIntegracaoService alertaIntegracaoService;

    /** Recalcula os alertas da gestante a partir do estado atual do checklist. */
    @PostMapping("/api/gestantes/{gestanteId}/alertas/avaliar")
    public ResponseEntity<List<AlertaResponse>> avaliar(@PathVariable Long gestanteId) {
        return ResponseEntity.ok(alertaIntegracaoService.reavaliar(gestanteId));
    }

    @GetMapping("/api/gestantes/{gestanteId}/alertas")
    public ResponseEntity<List<AlertaResponse>> listar(@PathVariable Long gestanteId) {
        return ResponseEntity.ok(alertaIntegracaoService.listar(gestanteId));
    }

    @GetMapping("/api/gestantes/{gestanteId}/alertas/resumo")
    public ResponseEntity<ResumoAlertasResponse> resumo(@PathVariable Long gestanteId) {
        return ResponseEntity.ok(alertaIntegracaoService.resumo(gestanteId));
    }

    @PatchMapping("/api/alertas/{alertaId}/leitura")
    public ResponseEntity<AlertaResponse> marcarLido(@PathVariable Long alertaId) {
        return ResponseEntity.ok(alertaIntegracaoService.marcarLido(alertaId));
    }

    @PatchMapping("/api/alertas/{alertaId}/resolucao")
    public ResponseEntity<AlertaResponse> resolver(@PathVariable Long alertaId) {
        return ResponseEntity.ok(alertaIntegracaoService.resolver(alertaId));
    }
}
