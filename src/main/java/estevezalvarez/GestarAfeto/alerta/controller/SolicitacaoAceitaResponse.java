package estevezalvarez.GestarAfeto.alerta.controller;

/**
 * Resposta de uma operacao aceita para processamento assincrono.
 *
 * <p>Acompanha o HTTP 202 e diz explicitamente que o trabalho foi agendado, nao concluido.
 * O {@code eventoId} nao e devolvido de proposito: quem chama nao deve acoplar-se ao
 * identificador interno da mensagem.</p>
 */
public record SolicitacaoAceitaResponse(Long gestanteId, String status, String mensagem) {

    public static SolicitacaoAceitaResponse reavaliacao(Long gestanteId) {
        return new SolicitacaoAceitaResponse(
            gestanteId,
            "ACEITO",
            "Reavaliacao solicitada. Os alertas serao atualizados em instantes.");
    }
}
