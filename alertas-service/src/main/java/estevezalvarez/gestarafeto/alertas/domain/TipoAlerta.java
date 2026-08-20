package estevezalvarez.gestarafeto.alertas.domain;

/**
 * Classifica o motivo pelo qual o alerta foi gerado pelo motor de regras.
 */
public enum TipoAlerta {
    /** Item pendente cuja janela recomendada ja foi ultrapassada. */
    PROCEDIMENTO_ATRASADO,
    /** Item pendente cuja janela recomendada esta em curso. */
    PROCEDIMENTO_PENDENTE,
    /** Item pendente cuja janela recomendada comeca em breve. */
    JANELA_PROXIMA,
    /** Item marcado como PRECISA_REVISAR no servico principal. */
    REVISAO_SOLICITADA
}
