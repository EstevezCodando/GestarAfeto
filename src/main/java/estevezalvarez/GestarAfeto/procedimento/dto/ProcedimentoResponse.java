package estevezalvarez.GestarAfeto.procedimento.dto;

import estevezalvarez.GestarAfeto.procedimento.domain.ProcedimentoPreNatal;
import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;
import estevezalvarez.GestarAfeto.procedimento.domain.TrimestreGestacional;

public record ProcedimentoResponse(
    Long id,
    String nome,
    String descricao,
    TipoProcedimento tipo,
    TrimestreGestacional trimestreRecomendado,
    Integer semanaInicialRecomendada,
    Integer semanaFinalRecomendada,
    boolean obrigatorio,
    boolean ativo
) {
    public static ProcedimentoResponse from(ProcedimentoPreNatal p) {
        return new ProcedimentoResponse(
            p.getId(), p.getNome(), p.getDescricao(), p.getTipo(),
            p.getTrimestreRecomendado(), p.getSemanaInicialRecomendada(),
            p.getSemanaFinalRecomendada(), p.isObrigatorio(), p.isAtivo()
        );
    }
}
