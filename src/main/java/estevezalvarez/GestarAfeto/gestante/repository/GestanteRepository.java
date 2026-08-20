package estevezalvarez.GestarAfeto.gestante.repository;

import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GestanteRepository extends JpaRepository<Gestante, Long> {
    Page<Gestante> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    Optional<Gestante> findByEmailIgnoreCase(String email);
}
