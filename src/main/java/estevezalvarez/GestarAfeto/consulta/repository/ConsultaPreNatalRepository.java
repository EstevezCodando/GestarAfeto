package estevezalvarez.GestarAfeto.consulta.repository;

import estevezalvarez.GestarAfeto.consulta.domain.ConsultaPreNatal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ConsultaPreNatalRepository extends JpaRepository<ConsultaPreNatal, Long> {
    @EntityGraph(attributePaths = "gestante")
    List<ConsultaPreNatal> findByGestanteIdOrderByDataConsultaDesc(Long gestanteId);

    @EntityGraph(attributePaths = "gestante")
    Page<ConsultaPreNatal> findByGestanteIdOrderByDataConsultaDesc(Long gestanteId, Pageable pageable);

    @EntityGraph(attributePaths = "gestante")
    List<ConsultaPreNatal> findByGestanteIdAndDataConsultaBetweenOrderByDataConsultaDesc(
        Long gestanteId,
        LocalDate inicio,
        LocalDate fim
    );

    void deleteByGestanteId(Long gestanteId);
}
