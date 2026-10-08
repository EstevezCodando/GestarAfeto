package estevezalvarez.gestarafeto.alertas.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sondas de disponibilidade e configuracoes criticas do perfil de producao. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorEConfiguracaoTest {

    @Autowired
    private MockMvc mockMvc;

    private Properties carregar(String recurso) throws IOException {
        Properties props = new Properties();
        try (InputStream in = new ClassPathResource(recurso).getInputStream()) {
            props.load(in);
        }
        return props;
    }

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
        for (String endpoint : new String[] {"env", "beans", "heapdump", "threaddump", "configprops"}) {
            mockMvc.perform(get("/actuator/" + endpoint)).andExpect(status().isNotFound());
        }
    }

    @Test
    void perfilProdNaoTemValorPadraoParaCredenciaisDoBanco() throws IOException {
        Properties prod = carregar("application-prod.properties");

        for (String chave : new String[] {
            "spring.datasource.url", "spring.datasource.username", "spring.datasource.password"}) {
            String valor = prod.getProperty(chave);
            assertTrue(valor.startsWith("${") && valor.endsWith("}"), chave + " deve vir do ambiente");
            assertFalse(valor.contains(":"), chave + " nao pode ter valor padrao embutido: " + valor);
        }
        assertEquals("never", prod.getProperty("management.endpoint.health.show-details"));
        assertEquals("validate", prod.getProperty("spring.jpa.hibernate.ddl-auto"));
    }

    @Test
    void exportacaoOtlpFicaDesligadaSalvoConfiguracaoExplicita() throws IOException {
        Properties base = carregar("application.properties");

        assertEquals("${OTEL_EXPORT_ENABLED:false}", base.getProperty("management.tracing.export.otlp.enabled"));
        assertEquals("${OTEL_EXPORT_ENABLED:false}", base.getProperty("management.logging.export.otlp.enabled"));
    }

    @Test
    void consumoDeMensagensConfirmaSoAposProcessarEEnviaFalhasParaDlq() throws IOException {
        Properties base = carregar("application.properties");

        assertEquals("auto", base.getProperty("spring.rabbitmq.listener.simple.acknowledge-mode"));
        assertEquals("false", base.getProperty("spring.rabbitmq.listener.simple.default-requeue-rejected"));
    }
}
