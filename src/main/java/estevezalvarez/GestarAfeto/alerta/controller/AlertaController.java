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
 *
 * <p>As rotas se dividem em dois regimes. A reavaliacao e <b>assincrona</b>: publica um
 * evento e responde 202. As consultas e as acoes sobre um alerta especifico continuam
 * <b>sincronas</b>, porque a tela precisa do resultado imediato.</p>
 */
@RestController
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaIntegracaoService alertaIntegracaoService;

    /**
     * Solicita a reavaliacao dos alertas da gestante.
     *
     * <p>Responde <b>202 Accepted</b>, e nao 200: o servico apenas publicou o evento. O
     * calculo acontece no microsservico, de forma assincrona, e o resultado aparece na
     * proxima consulta. Devolver 200 com a lista daria a entender que o processamento ja
     * terminou.</p>
     */
    @PostMapping("/api/gestantes/{gestanteId}/alertas/avaliar")
    public ResponseEntity<SolicitacaoAceitaResponse> avaliar(@PathVariable Long gestanteId) {
        alertaIntegracaoService.solicitarReavaliacao(gestanteId);
        return ResponseEntity.accepted().body(SolicitacaoAceitaResponse.reavaliacao(gestanteId));
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
