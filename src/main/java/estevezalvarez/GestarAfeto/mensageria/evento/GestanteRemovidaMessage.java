package estevezalvarez.GestarAfeto.mensageria.evento;

import estevezalvarez.GestarAfeto.mensageria.TopologiaEventos;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado apos a remocao de uma gestante, para que dados derivados mantidos por
 * outros servicos sejam descartados.
 *
 * <p>Aqui o identificador basta: a instrucao e "esqueca tudo desta gestante", e nao ha
 * estado a transportar.</p>
 */
public record GestanteRemovidaMessage(
    String eventoId,
    String tipo,
    int versao,
    Instant ocorridoEm,
    String origem,
    Long gestanteId
) {

    public static final String TIPO = "GESTANTE_REMOVIDA";
    public static final int VERSAO_ATUAL = 1;

    public static GestanteRemovidaMessage nova(Long gestanteId) {
        return new GestanteRemovidaMessage(
            UUID.randomUUID().toString(),
            TIPO,
            VERSAO_ATUAL,
            Instant.now(),
            TopologiaEventos.ORIGEM,
            gestanteId);
    }
}
