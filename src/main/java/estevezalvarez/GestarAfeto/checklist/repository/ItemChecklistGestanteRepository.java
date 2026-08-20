package estevezalvarez.GestarAfeto.checklist.repository;

import estevezalvarez.GestarAfeto.checklist.domain.ItemChecklistGestante;
import estevezalvarez.GestarAfeto.checklist.domain.StatusChecklist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemChecklistGestanteRepository extends JpaRepository<ItemChecklistGestante, Long> {
    @EntityGraph(attributePaths = "procedimento")
    List<ItemChecklistGestante> findByGestanteId(Long gestanteId);

    boolean existsByGestanteId(Long gestanteId);

    boolean existsByGestanteIdAndProcedimentoId(Long gestanteId, Long procedimentoId);

    @EntityGraph(attributePaths = "procedimento")
    List<ItemChecklistGestante> findByGestanteIdAndStatus(Long gestanteId, StatusChecklist status);

    @Override
    @EntityGraph(attributePaths = {"gestante", "procedimento"})
    Optional<ItemChecklistGestante> findById(Long id);

    void deleteByGestanteId(Long gestanteId);
}
