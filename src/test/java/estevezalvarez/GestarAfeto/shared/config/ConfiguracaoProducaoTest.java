package estevezalvarez.GestarAfeto.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Configuracoes criticas do perfil de producao. Sao verificacoes sobre os arquivos, sem subir
 * o contexto, para falhar rapido se alguem reintroduzir um segredo padrao ou abrir um endpoint.
 */
class ConfiguracaoProducaoTest {

    private Properties carregar(String recurso) throws IOException {
        Properties props = new Properties();
        try (InputStream in = new ClassPathResource(recurso).getInputStream()) {
            props.load(in);
        }
        return props;
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
    }

    @Test
    void perfilProdValidaOEsquemaEEscondeDetalhesDeSaude() throws IOException {
        Properties prod = carregar("application-prod.properties");

        assertEquals("validate", prod.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("never", prod.getProperty("management.endpoint.health.show-details"));
        assertEquals("false", prod.getProperty("spring.h2.console.enabled"));
    }

    @Test
    void actuatorExpoeApenasHealthEInfo() throws IOException {
        Properties base = carregar("application.properties");

        assertEquals("health,info", base.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("true", base.getProperty("management.endpoint.health.probes.enabled"));
    }

    @Test
    void exportacaoOtlpFicaDesligadaSalvoConfiguracaoExplicita() throws IOException {
        Properties base = carregar("application.properties");

        assertEquals("${OTEL_EXPORT_ENABLED:false}", base.getProperty("management.tracing.export.otlp.enabled"));
        assertEquals("${OTEL_EXPORT_ENABLED:false}", base.getProperty("management.logging.export.otlp.enabled"));
    }

    @Test
    void urlDoMicrosservicoDeAlertasVemDoAmbiente() throws IOException {
        Properties base = carregar("application.properties");

        assertTrue(base.getProperty("gestarafeto.alertas.url").startsWith("${GESTARAFETO_ALERTAS_URL"));
    }
}
