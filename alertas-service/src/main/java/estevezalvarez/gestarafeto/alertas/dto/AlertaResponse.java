package estevezalvarez.gestarafeto.alertas.dto;

import estevezalvarez.gestarafeto.alertas.domain.Alerta;
import estevezalvarez.gestarafeto.alertas.domain.PrioridadeAlerta;
import estevezalvarez.gestarafeto.alertas.domain.StatusAlerta;
import estevezalvarez.gestarafeto.alertas.domain.TipoAlerta;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AlertaResponse(
    Long id,
    Long gestanteId,
    String gestanteNome,
    Long origemId,
    TipoAlerta tipo,
    PrioridadeAlerta prioridade,
    StatusAlerta status,
    String titulo,
    String mensagem,
    LocalDate dataReferencia,
    Integer semanaGestacionalReferencia,
    LocalDateTime dataCriacao,
    LocalDateTime dataLeitura,
    LocalDateTime dataResolucao
) {
    public static AlertaResponse from(Alerta a) {
        return new AlertaResponse(
            a.getId(),
            a.getGestanteId(),
            a.getGestanteNome(),
            a.getOrigemId(),
            a.getTipo(),
            a.getPrioridade(),
            a.getStatus(),
            a.getTitulo(),
            a.getMensagem(),
            a.getDataReferencia(),
            a.getSemanaGestacionalReferencia(),
            a.getDataCriacao(),
            a.getDataLeitura(),
            a.getDataResolucao()
        );
    }
}
