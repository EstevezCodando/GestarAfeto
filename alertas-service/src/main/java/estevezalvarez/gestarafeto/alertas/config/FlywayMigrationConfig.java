package estevezalvarez.gestarafeto.alertas.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Garante que as migrations rodem antes da validacao do schema pelo Hibernate,
 * mesma abordagem usada no servico principal.
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
public class FlywayMigrationConfig {

    @Bean
    HibernatePropertiesCustomizer flywayBeforeJpa(DataSource dataSource, Environment environment) {
        return hibernateProperties -> Flyway.configure()
            .dataSource(dataSource)
            .locations(environment.getProperty("spring.flyway.locations", "classpath:db/migration"))
            .load()
            .migrate();
    }
}
