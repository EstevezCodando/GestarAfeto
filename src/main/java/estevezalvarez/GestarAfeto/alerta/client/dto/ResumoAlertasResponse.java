package estevezalvarez.GestarAfeto.alerta.client.dto;

/**
 * Contagens agregadas de alertas de uma gestante.
 */
public record ResumoAlertasResponse(
    Long gestanteId,
    long totalAtivos,
    long alta,
    long media,
    long baixa,
    long naoLidos,
    long resolvidos
) {
    public static ResumoAlertasResponse vazio(Long gestanteId) {
        return new ResumoAlertasResponse(gestanteId, 0, 0, 0, 0, 0, 0);
    }
}
