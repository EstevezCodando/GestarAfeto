package estevezalvarez.gestarafeto.alertas.domain;

public enum StatusAlerta {
    /** Alerta ativo e ainda nao visualizado. */
    ABERTO,
    /** Alerta ativo, ja visualizado pela equipe. */
    LIDO,
    /** Condicao que originou o alerta deixou de existir. */
    RESOLVIDO,
    /** Alerta descartado manualmente. */
    CANCELADO;

    public boolean isAtivo() {
        return this == ABERTO || this == LIDO;
    }
}
