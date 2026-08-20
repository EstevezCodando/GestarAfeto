package estevezalvarez.gestarafeto.alertas;

import estevezalvarez.gestarafeto.alertas.config.AlertaRegrasProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AlertasServiceApplicationTests {

    @Autowired
    private AlertaRegrasProperties regras;

    @Test
    void contextLoads() {
    }

    @Test
    void carregaParametrosDoMotorDeRegras() {
        assertEquals(2, regras.getAntecedenciaSemanas());
        assertEquals(40, regras.getSemanasGestacaoTotal());
        assertTrue(regras.isAlertarNaoObrigatorios());
    }
}
