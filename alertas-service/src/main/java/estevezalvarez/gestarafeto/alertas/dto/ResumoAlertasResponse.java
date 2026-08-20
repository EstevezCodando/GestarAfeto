package estevezalvarez.gestarafeto.alertas.dto;

/**
 * Contagens agregadas usadas pelo painel de alertas do front-end.
 */
public record ResumoAlertasResponse(
    Long gestanteId,
    long totalAtivos,
    long alta,
    long media,
    long baixa,
    long naoLidos,
    long resolvidos
) {}
