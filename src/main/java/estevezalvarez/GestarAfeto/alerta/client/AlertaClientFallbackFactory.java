package estevezalvarez.GestarAfeto.alerta.client;

import estevezalvarez.GestarAfeto.alerta.client.dto.AlertaResponse;
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
 *   <li><b>Leituras</b> devolvem vazio. O acompanhamento pre-natal continua funcionando sem
 *       o painel de alertas, que e um recurso complementar.</li>
 *   <li><b>Acoes explicitas da usuaria</b> (marcar como lido, resolver) falham com 503.
 *       Fingir sucesso faria a interface mostrar um estado que o microsservico nunca
 *       registrou.</li>
 * </ul>
 *
 * <p>Depois do TP4 este fallback cobre uma superficie menor, e de proposito. A reavaliacao e
 * a remocao nao passam mais por aqui: como sao publicadas no RabbitMQ, a indisponibilidade
 * do consumidor nao interrompe nada — as mensagens ficam enfileiradas ate ele voltar. O
 * circuit breaker protege apenas o que ainda e sincrono.</p>
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

            private ServicoIndisponivelException indisponivel() {
                return new ServicoIndisponivelException(
                    "O servico de alertas esta indisponivel no momento. Tente novamente em instantes.");
            }
        };
    }
}
