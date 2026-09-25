package estevezalvarez.GestarAfeto.alerta.client;

import estevezalvarez.GestarAfeto.alerta.client.dto.AlertaResponse;
import estevezalvarez.GestarAfeto.alerta.client.dto.ResumoAlertasResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Cliente declarativo (Spring Cloud OpenFeign) do microsservico de alertas.
 *
 * <p>Esta interface encolheu no TP4, e o que saiu dela e a evidencia da refatoracao. As
 * operacoes de <b>escrita</b> que antes passavam por aqui ({@code avaliar} e
 * {@code removerPorGestante}) viraram eventos publicados no RabbitMQ: o servico principal
 * nao chama mais o microsservico para provocar trabalho, apenas anuncia o que aconteceu.</p>
 *
 * <p>Restaram as operacoes de <b>leitura</b> e as acoes sobre um alerta ja existente, que
 * sao sincronas por natureza: a tela precisa da resposta imediata e o usuario precisa saber
 * se a acao foi aceita. Essas continuam com circuit breaker e fallback.</p>
 */
@FeignClient(
    name = "gestarafeto-alertas",
    url = "${gestarafeto.alertas.url:http://localhost:8081}",
    fallbackFactory = AlertaClientFallbackFactory.class
)
public interface AlertaClient {

    @GetMapping("/api/alertas/gestante/{gestanteId}")
    List<AlertaResponse> listarPorGestante(@PathVariable("gestanteId") Long gestanteId);

    @GetMapping("/api/alertas/gestante/{gestanteId}/resumo")
    ResumoAlertasResponse resumo(@PathVariable("gestanteId") Long gestanteId);

    @PatchMapping("/api/alertas/{id}/leitura")
    AlertaResponse marcarLido(@PathVariable("id") Long id);

    @PatchMapping("/api/alertas/{id}/resolucao")
    AlertaResponse resolver(@PathVariable("id") Long id);
}
