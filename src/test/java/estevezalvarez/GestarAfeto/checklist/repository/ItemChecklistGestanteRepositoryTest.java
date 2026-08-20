package estevezalvarez.GestarAfeto.checklist.repository;

import estevezalvarez.GestarAfeto.checklist.domain.ItemChecklistGestante;
import estevezalvarez.GestarAfeto.checklist.domain.StatusChecklist;
import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import estevezalvarez.GestarAfeto.procedimento.domain.ProcedimentoPreNatal;
import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class ItemChecklistGestanteRepositoryTest {

    @Autowired
    private ItemChecklistGestanteRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void impedeItemDuplicadoParaMesmaGestanteEProcedimento() {
        Gestante gestante = entityManager.persist(Gestante.builder()
            .nome("Carla")
            .dataProvavelParto(LocalDate.now().plusMonths(4))
            .build());
        ProcedimentoPreNatal procedimento = entityManager.persist(ProcedimentoPreNatal.builder()
            .nome("Hemograma")
            .tipo(TipoProcedimento.EXAME)
            .obrigatorio(true)
            .ativo(true)
            .build());

        repository.saveAndFlush(ItemChecklistGestante.builder()
            .gestante(gestante)
            .procedimento(procedimento)
            .status(StatusChecklist.PENDENTE)
            .build());

        assertThrows(RuntimeException.class, () -> repository.saveAndFlush(ItemChecklistGestante.builder()
            .gestante(gestante)
            .procedimento(procedimento)
            .status(StatusChecklist.PENDENTE)
            .build()));
    }

    @Test
    void listaChecklistPorGestanteEStatus() {
        Gestante gestante = entityManager.persist(Gestante.builder()
            .nome("Julia")
            .dataProvavelParto(LocalDate.now().plusMonths(5))
            .build());
        ProcedimentoPreNatal procedimento = entityManager.persist(ProcedimentoPreNatal.builder()
            .nome("Consulta")
            .tipo(TipoProcedimento.CONSULTA)
            .obrigatorio(true)
            .ativo(true)
            .build());
        entityManager.persist(ItemChecklistGestante.builder()
            .gestante(gestante)
            .procedimento(procedimento)
            .status(StatusChecklist.REALIZADO)
            .dataRealizacao(LocalDate.now())
            .build());
        entityManager.flush();

        assertTrue(repository.existsByGestanteId(gestante.getId()));
        assertEquals(1, repository.findByGestanteIdAndStatus(gestante.getId(), StatusChecklist.REALIZADO).size());
    }
}
