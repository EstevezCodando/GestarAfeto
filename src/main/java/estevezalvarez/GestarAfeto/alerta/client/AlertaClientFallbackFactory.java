package estevezalvarez.GestarAfeto.alerta.client;

import estevezalvarez.GestarAfeto.alerta.client.dto.AlertaResponse;
import estevezalvarez.GestarAfeto.alerta.client.dto.AvaliarChecklistRequest;
import estevezalvarez.GestarAfeto.alerta.client.dto.ResumoAlertasResponse;
import estevezalvarez.GestarAfeto.shared.exception.ServicoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Comportamento do servico principal quando o microsservico de alertas nao responde.
 *
 * <p>A degradacao e deliberadamente assimetrica:</p>
 * <ul>
 *   <li><b>Leituras e reavaliacoes</b> devolvem vazio. O acompanhamento pre-natal continua
 *       funcionando sem o painel de alertas, que e um recurso complementar.</li>
 *   <li><b>Acoes explicitas da usuaria</b> (marcar como lido, resolver) falham com 503.
 *       Fingir sucesso faria a interface mostrar um estado que o microsservico nunca
 *       registrou.</li>
 * </ul>
 */
@Component
public class AlertaClientFallbackFactory implements FallbackFactory<AlertaClient> {

    private static final Logger log = LoggerFactory.getLogger(AlertaClientFallbackFactory.class);

    @Override
    public AlertaClient create(Throwable cause) {
        log.warn("Microsservico de alertas indisponivel; aplicando fallback. Causa: {}",
            cause == null ? "desconhecida" : cause.toString());

        return new AlertaClient() {

            @Override
            public List<AlertaResponse> avaliar(AvaliarChecklistRequest request) {
                return List.of();
            }

            @Override
            public List<AlertaResponse> listarPorGestante(Long gestanteId) {
                return List.of();
            }

            @Override
            public ResumoAlertasResponse resumo(Long gestanteId) {
                return ResumoAlertasResponse.vazio(gestanteId);
            }

            @Override
            public AlertaResponse marcarLido(Long id) {
                throw indisponivel();
            }

            @Override
            public AlertaResponse resolver(Long id) {
                throw indisponivel();
            }

            @Override
            public void removerPorGestante(Long gestanteId) {
                // A remocao da gestante nao pode ser bloqueada por um servico auxiliar.
                // Os alertas orfaos sao descartados na proxima reavaliacao daquele id.
                log.warn("Nao foi possivel remover alertas da gestante {}: microsservico indisponivel.",
                    gestanteId);
            }

            private ServicoIndisponivelException indisponivel() {
                return new ServicoIndisponivelException(
                    "O servico de alertas esta indisponivel no momento. Tente novamente em instantes.");
            }
        };
    }
}
