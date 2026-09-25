package estevezalvarez.gestarafeto.alertas.mensageria;

/**
 * Nomes da topologia RabbitMQ usada por este microsservico.
 *
 * <p>O exchange e as routing keys sao replicados do servico principal, pela mesma razao que
 * os DTOs do contrato REST: um artefato compartilhado acoplaria o ciclo de entrega dos dois
 * servicos. As filas e os bindings, ao contrario, sao exclusivos deste consumidor — e ele
 * quem decide o que quer receber.</p>
 */
public final class TopologiaEventos {

    // ------------------------------------------------------------ acordado com o produtor

    public static final String EXCHANGE = "gestarafeto.eventos";
    public static final String EXCHANGE_DLX = "gestarafeto.eventos.dlx";

    public static final String RK_CHECKLIST_ALTERADO = "checklist.alterado";
    public static final String RK_GESTANTE_REMOVIDA = "gestante.removida";

    // ------------------------------------------------------------------ deste consumidor

    /**
     * Uma fila por evento, e nao uma unica fila para tudo. Assim uma mensagem de checklist
     * travada nao atrasa a limpeza de uma gestante removida, e cada fluxo pode ter a propria
     * concorrencia e a propria politica de repeticao.
     */
    public static final String FILA_CHECKLIST_ALTERADO = "alertas.checklist-alterado";
    public static final String FILA_GESTANTE_REMOVIDA = "alertas.gestante-removida";

    /** Destino das mensagens que falharam em definitivo, para inspecao e reprocessamento. */
    public static final String FILA_CHECKLIST_ALTERADO_DLQ = "alertas.checklist-alterado.dlq";
    public static final String FILA_GESTANTE_REMOVIDA_DLQ = "alertas.gestante-removida.dlq";

    private TopologiaEventos() {
    }
}
