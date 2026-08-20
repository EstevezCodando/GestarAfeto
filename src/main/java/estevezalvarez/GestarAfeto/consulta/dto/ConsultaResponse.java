package estevezalvarez.GestarAfeto.consulta.dto;

import estevezalvarez.GestarAfeto.consulta.domain.ConsultaPreNatal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConsultaResponse(
    Long id,
    Long gestanteId,
    String nomeGestante,
    LocalDate dataConsulta,
    Integer semanaGestacional,
    BigDecimal peso,
    String pressaoArterial,
    String observacoes,
    LocalDateTime dataRegistro
) {
    public static ConsultaResponse from(ConsultaPreNatal c) {
        return new ConsultaResponse(
            c.getId(), c.getGestante().getId(), c.getGestante().getNome(),
            c.getDataConsulta(), c.getSemanaGestacional(), c.getPeso(),
            c.getPressaoArterial(), c.getObservacoes(), c.getDataRegistro()
        );
    }
}
