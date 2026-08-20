package estevezalvarez.GestarAfeto.gestante.controller;

import estevezalvarez.GestarAfeto.gestante.dto.CriarGestanteRequest;
import estevezalvarez.GestarAfeto.gestante.dto.GestanteResponse;
import estevezalvarez.GestarAfeto.gestante.service.GestanteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gestantes")
@RequiredArgsConstructor
public class GestanteController {

    private final GestanteService gestanteService;

    @PostMapping
    public ResponseEntity<GestanteResponse> cadastrar(@Valid @RequestBody CriarGestanteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gestanteService.cadastrar(request));
    }

    @GetMapping
    public ResponseEntity<List<GestanteResponse>> listar() {
        return ResponseEntity.ok(gestanteService.listar());
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<GestanteResponse>> listarPaginado(
            @RequestParam(required = false) String nome,
            Pageable pageable) {
        return ResponseEntity.ok(gestanteService.listarPaginado(nome, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GestanteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(gestanteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GestanteResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CriarGestanteRequest request) {
        return ResponseEntity.ok(gestanteService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        gestanteService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
