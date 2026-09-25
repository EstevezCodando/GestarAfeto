package estevezalvarez.GestarAfeto.mensageria.evento;

import estevezalvarez.GestarAfeto.mensageria.TopologiaEventos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Evento publicado quando o checklist de uma gestante muda.
 *
 * <p>A mensagem carrega o <b>estado completo</b> do checklist, e nao apenas o id da
 * gestante. E a diferenca central em relacao a integracao do TP3: o consumidor consegue
 * processar o evento sem nenhuma chamada de volta ao produtor, o que elimina a dependencia
 * de disponibilidade entre os dois servicos no momento do processamento.</p>
 *
 * <p>Os quatro primeiros campos formam o envelope comum a todos os eventos do sistema:</p>
 * <ul>
 *   <li>{@code eventoId} identifica a ocorrencia e permite rastrear a mensagem nos logs dos
 *       dois lados, alem de servir de base para deduplicacao;</li>
 *   <li>{@code tipo} identifica o evento sem depender do nome da classe Java;</li>
 *   <li>{@code versao} permite evoluir o contrato mantendo consumidores antigos;</li>
 *   <li>{@code ocorridoEm} e {@code origem} dao o contexto temporal e o emissor.</li>
 * </ul>
 */
public record ChecklistAlteradoMessage(
    String eventoId,
    String tipo,
    int versao,
    Instant ocorridoEm,
    String origem,
    Long gestanteId,
    String gestanteNome,
    LocalDate dataUltimaMenstruacao,
    LocalDate dataProvavelParto,
    List<ItemChecklistEvento> itens
) {

    public static final String TIPO = "CHECKLIST_ALTERADO";
    public static final int VERSAO_ATUAL = 1;

    public static ChecklistAlteradoMessage nova(
            Long gestanteId,
            String gestanteNome,
            LocalDate dataUltimaMenstruacao,
            LocalDate dataProvavelParto,
            List<ItemChecklistEvento> itens) {
        return new ChecklistAlteradoMessage(
            UUID.randomUUID().toString(),
            TIPO,
            VERSAO_ATUAL,
            Instant.now(),
            TopologiaEventos.ORIGEM,
            gestanteId,
            gestanteNome,
            dataUltimaMenstruacao,
            dataProvavelParto,
            itens);
    }
}
