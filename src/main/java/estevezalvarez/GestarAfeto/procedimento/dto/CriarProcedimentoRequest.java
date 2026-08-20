package estevezalvarez.GestarAfeto.procedimento.dto;

import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;
import estevezalvarez.GestarAfeto.procedimento.domain.TrimestreGestacional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarProcedimentoRequest(
    @NotBlank(message = "Nome é obrigatório") String nome,
    String descricao,
    @NotNull(message = "Tipo é obrigatório") TipoProcedimento tipo,
    TrimestreGestacional trimestreRecomendado,
    Integer semanaInicialRecomendada,
    Integer semanaFinalRecomendada,
    boolean obrigatorio
) {}
