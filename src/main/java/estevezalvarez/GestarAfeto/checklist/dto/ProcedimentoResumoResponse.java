package estevezalvarez.GestarAfeto.checklist.dto;

import estevezalvarez.GestarAfeto.procedimento.domain.ProcedimentoPreNatal;
import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;

public record ProcedimentoResumoResponse(Long id, String nome, TipoProcedimento tipo) {
    public static ProcedimentoResumoResponse from(ProcedimentoPreNatal p) {
        return new ProcedimentoResumoResponse(p.getId(), p.getNome(), p.getTipo());
    }
}
