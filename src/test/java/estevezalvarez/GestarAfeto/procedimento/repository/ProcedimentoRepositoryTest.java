package estevezalvarez.GestarAfeto.procedimento.repository;

import estevezalvarez.GestarAfeto.procedimento.domain.ProcedimentoPreNatal;
import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;
import estevezalvarez.GestarAfeto.procedimento.domain.TrimestreGestacional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class ProcedimentoRepositoryTest {

    @Autowired
    private ProcedimentoRepository repository;

    @Test
    void filtraProcedimentosAtivosPorTipoTrimestreESemana() {
        repository.save(ProcedimentoPreNatal.builder()
            .nome("Hemograma")
            .tipo(TipoProcedimento.EXAME)
            .trimestreRecomendado(TrimestreGestacional.PRIMEIRO)
            .semanaInicialRecomendada(1)
            .semanaFinalRecomendada(13)
            .obrigatorio(true)
            .ativo(true)
            .build());
        repository.save(ProcedimentoPreNatal.builder()
            .nome("Procedimento inativo")
            .tipo(TipoProcedimento.EXAME)
            .obrigatorio(true)
            .ativo(false)
            .build());

        assertEquals(1, repository.findByAtivoTrue(PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, repository.findByTipoAndAtivoTrue(TipoProcedimento.EXAME, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, repository.findByTrimestreRecomendadoAndAtivoTrue(TrimestreGestacional.PRIMEIRO, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, repository.findAtivosRecomendadosNaSemana(10).size());
    }
}
