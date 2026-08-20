package estevezalvarez.gestarafeto.alertas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Recorte de um item de checklist enviado pelo servico principal.
 *
 * <p>{@code status} e {@code procedimentoTipo} trafegam como texto de proposito: os dois
 * servicos evoluem seus enums de forma independente e um valor desconhecido nao deve quebrar
 * a integracao.</p>
 */
public record ItemChecklistSnapshot(
    @NotNull Long itemId,
    @NotBlank String procedimentoNome,
    String procedimentoTipo,
    @NotBlank String status,
    boolean obrigatorio,
    Integer semanaInicialRecomendada,
    Integer semanaFinalRecomendada,
    LocalDate dataRealizacao
) {}
