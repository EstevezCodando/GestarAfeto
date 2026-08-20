package estevezalvarez.gestarafeto.alertas;

import estevezalvarez.gestarafeto.alertas.config.AlertaRegrasProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AlertaRegrasProperties.class)
public class AlertasServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertasServiceApplication.class, args);
    }
}
