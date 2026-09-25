package estevezalvarez.gestarafeto.alertas.mensageria.evento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Evento recebido quando o checklist de uma gestante muda no servico principal.
 *
 * <p>A mensagem traz o estado completo do checklist, e nao apenas o id da gestante. Por
 * isso este microsservico processa o evento sem nenhuma chamada de volta: nao precisa que o
 * produtor esteja no ar no momento do consumo.</p>
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
}
