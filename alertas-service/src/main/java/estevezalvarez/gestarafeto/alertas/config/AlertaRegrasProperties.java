package estevezalvarez.gestarafeto.alertas.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parametros do motor de regras de alerta.
 *
 * <p>Configuracao distribuida: o {@code ConfigurationPropertiesRebinder} do spring-cloud-context
 * reaplica os valores deste bean quando um {@code POST /actuator/refresh} e disparado, sem
 * reiniciar o servico. Combinado com o Config Server (habilitavel por
 * {@code GESTARAFETO_CONFIG_ENABLED=true}), permite ajustar as regras de alerta em runtime.
 * Sem Config Server, os valores vem de {@code application.properties} normalmente.</p>
 *
 * <p>O rebind acontece sobre a mesma instancia do bean, entao o {@code MotorDeRegrasAlerta}
 * enxerga os novos valores sem precisar ser recriado.</p>
 */
@Validated
@ConfigurationProperties(prefix = "gestarafeto.alertas.regras")
public class AlertaRegrasProperties {

    /** Quantas semanas antes do inicio da janela recomendada o alerta preventivo aparece. */
    @Min(0)
    @Max(20)
    private int antecedenciaSemanas = 2;

    /** Duracao considerada de uma gestacao a termo, usada para estimar a semana a partir da DPP. */
    @Min(20)
    @Max(45)
    private int semanasGestacaoTotal = 40;

    /** Quando falso, procedimentos nao obrigatorios nao geram alerta. */
    private boolean alertarNaoObrigatorios = true;

    public int getAntecedenciaSemanas() {
        return antecedenciaSemanas;
    }

    public void setAntecedenciaSemanas(int antecedenciaSemanas) {
        this.antecedenciaSemanas = antecedenciaSemanas;
    }

    public int getSemanasGestacaoTotal() {
        return semanasGestacaoTotal;
    }

    public void setSemanasGestacaoTotal(int semanasGestacaoTotal) {
        this.semanasGestacaoTotal = semanasGestacaoTotal;
    }

    public boolean isAlertarNaoObrigatorios() {
        return alertarNaoObrigatorios;
    }

    public void setAlertarNaoObrigatorios(boolean alertarNaoObrigatorios) {
        this.alertarNaoObrigatorios = alertarNaoObrigatorios;
    }
}
