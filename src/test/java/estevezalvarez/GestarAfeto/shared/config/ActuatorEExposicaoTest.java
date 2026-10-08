package estevezalvarez.GestarAfeto.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de inicializacao e disponibilidade: as sondas que o Docker e o Kubernetes consultam
 * precisam existir, e o que nao deve ser publico nao pode estar exposto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorEExposicaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void sondaDeLivenessResponde() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void sondaDeReadinessResponde() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void endpointsSensiveisDoActuatorNaoSaoExpostos() throws Exception {
        for (String endpoint : new String[] {"env", "beans", "heapdump", "threaddump", "configprops", "mappings"}) {
            mockMvc.perform(get("/actuator/" + endpoint))
                .andExpect(status().isNotFound());
        }
    }

    /**
     * Sem um UserDetailsService proprio, o Spring Boot gera um usuario e escreve a senha no log
     * ("Using generated security password"), que seguiria para o Loki.
     */
    @Test
    void naoExisteUsuarioPadraoComSenhaGeradaNoLog() {
        assertThrows(UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("user"));
    }
}
