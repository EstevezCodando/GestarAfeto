package estevezalvarez.GestarAfeto.alerta.client;

import estevezalvarez.GestarAfeto.alerta.client.dto.AlertaResponse;
import estevezalvarez.GestarAfeto.alerta.client.dto.AvaliarChecklistRequest;
import estevezalvarez.GestarAfeto.alerta.client.dto.ResumoAlertasResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Cliente declarativo (Spring Cloud OpenFeign) do microsservico de alertas.
 *
 * <p>A URL vem de {@code gestarafeto.alertas.url}, permitindo apontar para localhost em
 * desenvolvimento e para o nome do servico em um ambiente conteinerizado. Com
 * {@code spring.cloud.openfeign.circuitbreaker.enabled=true}, cada chamada passa por um
 * circuit breaker Resilience4j e cai em {@link AlertaClientFallbackFactory} quando o
 * microsservico esta indisponivel ou lento.</p>
 */
@FeignClient(
    name = "gestarafeto-alertas",
    url = "${gestarafeto.alertas.url:http://localhost:8081}",
    fallbackFactory = AlertaClientFallbackFactory.class
)
public interface AlertaClient {

    @PostMapping("/api/alertas/avaliacoes")
    List<AlertaResponse> avaliar(@RequestBody AvaliarChecklistRequest request);

    @GetMapping("/api/alertas/gestante/{gestanteId}")
    List<AlertaResponse> listarPorGestante(@PathVariable("gestanteId") Long gestanteId);

    @GetMapping("/api/alertas/gestante/{gestanteId}/resumo")
    ResumoAlertasResponse resumo(@PathVariable("gestanteId") Long gestanteId);

    @PatchMapping("/api/alertas/{id}/leitura")
    AlertaResponse marcarLido(@PathVariable("id") Long id);

    @PatchMapping("/api/alertas/{id}/resolucao")
    AlertaResponse resolver(@PathVariable("id") Long id);

    @DeleteMapping("/api/alertas/gestante/{gestanteId}")
    void removerPorGestante(@PathVariable("gestanteId") Long gestanteId);
}
