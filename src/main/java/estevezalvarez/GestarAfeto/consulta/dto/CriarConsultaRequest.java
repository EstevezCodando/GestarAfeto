package estevezalvarez.GestarAfeto.consulta.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CriarConsultaRequest(
    @NotNull(message = "Data da consulta é obrigatória") LocalDate dataConsulta,
    Integer semanaGestacional,
    BigDecimal peso,
    String pressaoArterial,
    String observacoes
) {}
