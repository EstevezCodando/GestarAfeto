package estevezalvarez.GestarAfeto.mensageria;

/**
 * Nomes da topologia RabbitMQ compartilhada entre os servicos.
 *
 * <p>O servico principal declara apenas o <b>exchange</b>: ele publica sem saber quem
 * escuta. Filas e bindings sao declarados por cada consumidor, que e quem decide o que
 * quer receber. Essa divisao e o que permite acrescentar um novo consumidor sem tocar
 * no produtor.</p>
 *
 * <p>As constantes sao replicadas no microsservico de alertas, pela mesma razao que os
 * DTOs do contrato REST sao: um artefato compartilhado acoplaria o ciclo de entrega dos
 * dois servicos.</p>
 */
public final class TopologiaEventos {

    /**
     * Exchange do tipo <b>topic</b>. A escolha e deliberada: com routing keys hierarquicas
     * ({@code dominio.acao}), um consumidor futuro pode assinar {@code checklist.*} ou
     * {@code gestante.#} sem que o produtor precise ser alterado. Um {@code direct} exigiria
     * um binding exato por evento; um {@code fanout} entregaria tudo a todos.
     */
    public static final String EXCHANGE = "gestarafeto.eventos";

    /** Exchange de dead letter: recebe o que os consumidores rejeitaram em definitivo. */
    public static final String EXCHANGE_DLX = "gestarafeto.eventos.dlx";

    /** Estado do checklist de uma gestante mudou e os alertas precisam ser reavaliados. */
    public static final String RK_CHECKLIST_ALTERADO = "checklist.alterado";

    /** Gestante removida: dados derivados em outros servicos devem ser descartados. */
    public static final String RK_GESTANTE_REMOVIDA = "gestante.removida";

    /** Identifica a origem das mensagens publicadas por este servico. */
    public static final String ORIGEM = "gestarafeto-principal";

    private TopologiaEventos() {
    }
}
