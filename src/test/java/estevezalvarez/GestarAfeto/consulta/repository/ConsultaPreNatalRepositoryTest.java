package estevezalvarez.GestarAfeto.consulta.repository;

import estevezalvarez.GestarAfeto.consulta.domain.ConsultaPreNatal;
import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
class ConsultaPreNatalRepositoryTest {

    @Autowired
    private ConsultaPreNatalRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void listaConsultasDaMaisRecenteParaMaisAntiga() {
        Gestante gestante = entityManager.persist(Gestante.builder()
            .nome("Paula")
            .dataProvavelParto(LocalDate.now().plusMonths(2))
            .build());
        entityManager.persist(ConsultaPreNatal.builder()
            .gestante(gestante)
            .dataConsulta(LocalDate.of(2026, 7, 1))
            .peso(new BigDecimal("70.10"))
            .build());
        entityManager.persist(ConsultaPreNatal.builder()
            .gestante(gestante)
            .dataConsulta(LocalDate.of(2026, 7, 15))
            .peso(new BigDecimal("70.80"))
            .build());
        entityManager.flush();

        var consultas = repository.findByGestanteIdOrderByDataConsultaDesc(gestante.getId());

        assertEquals(LocalDate.of(2026, 7, 15), consultas.getFirst().getDataConsulta());
    }

    @Test
    void rejeitaPesoInvalidoPorConstraintDeBanco() {
        Gestante gestante = entityManager.persist(Gestante.builder()
            .nome("Bianca")
            .dataProvavelParto(LocalDate.now().plusMonths(1))
            .build());
        assertThrows(RuntimeException.class, () -> {
            entityManager.persist(ConsultaPreNatal.builder()
                .gestante(gestante)
                .dataConsulta(LocalDate.now())
                .peso(BigDecimal.ZERO)
                .build());
            entityManager.flush();
        });
    }
}
