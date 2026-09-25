package estevezalvarez.gestarafeto.alertas.mensageria.evento;

import java.time.LocalDate;

/**
 * Recorte de um item de checklist recebido dentro de {@link ChecklistAlteradoMessage}.
 *
 * <p>Campos de texto em vez de enums: um valor desconhecido enviado por uma versao mais
 * nova do produtor nao pode impedir a desserializacao da mensagem inteira.</p>
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
