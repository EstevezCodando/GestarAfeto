package estevezalvarez.GestarAfeto.mensageria.evento;

import java.time.LocalDate;

/**
 * Recorte de um item de checklist transportado dentro de {@link ChecklistAlteradoMessage}.
 *
 * <p>O status e o tipo do procedimento trafegam como texto de proposito: os dois servicos
 * evoluem seus enums de forma independente e um valor desconhecido nao deve impedir a
 * desserializacao da mensagem inteira.</p>
 */
public record ItemChecklistEvento(
    Long itemId,
    String procedimentoNome,
    String procedimentoTipo,
    String status,
    boolean obrigatorio,
    Integer semanaInicialRecomendada,
    Integer semanaFinalRecomendada,
    LocalDate dataRealizacao
) {
}
