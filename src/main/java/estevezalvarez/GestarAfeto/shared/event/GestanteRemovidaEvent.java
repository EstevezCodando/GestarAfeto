package estevezalvarez.GestarAfeto.shared.event;

/**
 * Publicado apos a remocao de uma gestante, para que dados derivados mantidos por outros
 * servicos sejam descartados.
 */
public record GestanteRemovidaEvent(Long gestanteId) {
}
