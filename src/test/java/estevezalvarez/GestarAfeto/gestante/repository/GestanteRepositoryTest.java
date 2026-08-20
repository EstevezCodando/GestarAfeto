package estevezalvarez.GestarAfeto.gestante.repository;

import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class GestanteRepositoryTest {

    @Autowired
    private GestanteRepository repository;

    @Test
    void pesquisaGestantePorNomeComPaginacao() {
        repository.save(Gestante.builder()
            .nome("Maria Silva")
            .email("maria@example.com")
            .dataProvavelParto(LocalDate.now().plusMonths(3))
            .build());
        repository.save(Gestante.builder()
            .nome("Ana Souza")
            .dataProvavelParto(LocalDate.now().plusMonths(2))
            .build());

        var pagina = repository.findByNomeContainingIgnoreCase("maria", PageRequest.of(0, 10));

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Maria Silva", pagina.getContent().getFirst().getNome());
        assertTrue(repository.findByEmailIgnoreCase("MARIA@example.com").isPresent());
    }
}
