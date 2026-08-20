package estevezalvarez.GestarAfeto.consulta.controller;

import estevezalvarez.GestarAfeto.consulta.dto.ConsultaResponse;
import estevezalvarez.GestarAfeto.consulta.dto.CriarConsultaRequest;
import estevezalvarez.GestarAfeto.consulta.service.ConsultaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ConsultaController {

    private final ConsultaService consultaService;

    @PostMapping("/api/gestantes/{gestanteId}/consultas")
    public ResponseEntity<ConsultaResponse> registrar(
            @PathVariable Long gestanteId,
            @Valid @RequestBody CriarConsultaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.registrar(gestanteId, request));
    }

    @GetMapping("/api/gestantes/{gestanteId}/consultas")
    public ResponseEntity<List<ConsultaResponse>> listarPorGestante(@PathVariable Long gestanteId) {
        return ResponseEntity.ok(consultaService.listarPorGestante(gestanteId));
    }

    @GetMapping("/api/gestantes/{gestanteId}/consultas/paginado")
    public ResponseEntity<Page<ConsultaResponse>> listarPorGestantePaginado(
            @PathVariable Long gestanteId,
            Pageable pageable) {
        return ResponseEntity.ok(consultaService.listarPorGestantePaginado(gestanteId, pageable));
    }

    @GetMapping("/api/consultas/{id}")
    public ResponseEntity<ConsultaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(consultaService.buscarPorId(id));
    }

    @PutMapping("/api/consultas/{id}")
    public ResponseEntity<ConsultaResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CriarConsultaRequest request) {
        return ResponseEntity.ok(consultaService.atualizar(id, request));
    }

    @DeleteMapping("/api/consultas/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        consultaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
