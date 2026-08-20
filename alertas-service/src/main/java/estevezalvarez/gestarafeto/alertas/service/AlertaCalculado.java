package estevezalvarez.gestarafeto.alertas.service;

import estevezalvarez.gestarafeto.alertas.domain.PrioridadeAlerta;
import estevezalvarez.gestarafeto.alertas.domain.TipoAlerta;

import java.time.LocalDate;

/**
 * Resultado puro do motor de regras, antes de qualquer persistencia.
 * O par {@code (origemId, tipo)} identifica o alerta de forma estavel entre avaliacoes.
 */
public record AlertaCalculado(
    Long origemId,
    TipoAlerta tipo,
    PrioridadeAlerta prioridade,
    String titulo,
    String mensagem,
    LocalDate dataReferencia,
    Integer semanaGestacionalReferencia
) {
    public String chave() {
        return origemId + "|" + tipo.name();
    }
}
