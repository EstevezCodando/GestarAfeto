package estevezalvarez.GestarAfeto.alerta.client.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Alerta retornado pelo microsservico. Os enums do microsservico chegam como texto para
 * manter o servico principal tolerante a novos valores.
 */
public record AlertaResponse(
    Long id,
    Long gestanteId,
    String gestanteNome,
    Long origemId,
    String tipo,
    String prioridade,
    String status,
    String titulo,
    String mensagem,
    LocalDate dataReferencia,
    Integer semanaGestacionalReferencia,
    LocalDateTime dataCriacao,
    LocalDateTime dataLeitura,
    LocalDateTime dataResolucao
) {}
