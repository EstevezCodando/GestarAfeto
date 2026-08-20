package estevezalvarez.GestarAfeto.alerta.client.dto;

import java.time.LocalDate;

/**
 * Recorte de um item de checklist enviado ao microsservico de alertas.
 *
 * <p>Copia local do contrato publicado pelo microsservico. Os dois servicos nao compartilham
 * um jar comum de propósito: cada lado evolui seus tipos internos livremente, e {@code status}
 * e {@code procedimentoTipo} trafegam como texto para que um valor novo de um lado nao quebre
 * a desserializacao do outro.</p>
 */
public record ItemChecklistSnapshot(
    Long itemId,
    String procedimentoNome,
    String procedimentoTipo,
    String status,
    boolean obrigatorio,
    Integer semanaInicialRecomendada,
    Integer semanaFinalRecomendada,
    LocalDate dataRealizacao
) {}
