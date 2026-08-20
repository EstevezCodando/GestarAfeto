package estevezalvarez.GestarAfeto.gestante.dto;

import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GestanteResponse(
    Long id,
    String nome,
    LocalDate dataNascimento,
    String telefone,
    String email,
    LocalDate dataUltimaMenstruacao,
    LocalDate dataProvavelParto,
    String observacoes,
    LocalDateTime dataCadastro
) {
    public static GestanteResponse from(Gestante g) {
        return new GestanteResponse(
            g.getId(), g.getNome(), g.getDataNascimento(), g.getTelefone(),
            g.getEmail(), g.getDataUltimaMenstruacao(), g.getDataProvavelParto(),
            g.getObservacoes(), g.getDataCadastro()
        );
    }
}
