package estevezalvarez.GestarAfeto.gestante.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CriarGestanteRequest(
    @NotBlank(message = "Nome é obrigatório") String nome,
    LocalDate dataNascimento,
    String telefone,
    String email,
    LocalDate dataUltimaMenstruacao,
    LocalDate dataProvavelParto,
    String observacoes
) {}
