package estevezalvarez.GestarAfeto.procedimento.controller;

import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;
import estevezalvarez.GestarAfeto.procedimento.domain.TrimestreGestacional;
import estevezalvarez.GestarAfeto.procedimento.dto.CriarProcedimentoRequest;
import estevezalvarez.GestarAfeto.procedimento.dto.ProcedimentoResponse;
import estevezalvarez.GestarAfeto.procedimento.service.ProcedimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedimentos")
@RequiredArgsConstructor
public class ProcedimentoController {

    private final ProcedimentoService procedimentoService;

    @PostMapping
    public ResponseEntity<ProcedimentoResponse> cadastrar(@Valid @RequestBody CriarProcedimentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procedimentoService.cadastrar(request));
    }

    @GetMapping
    public ResponseEntity<List<ProcedimentoResponse>> listar() {
        return ResponseEntity.ok(procedimentoService.listar());
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<ProcedimentoResponse>> listarPaginado(
            @RequestParam(required = false) TipoProcedimento tipo,
            @RequestParam(required = false) TrimestreGestacional trimestre,
            @RequestParam(required = false) String nome,
            Pageable pageable) {
        return ResponseEntity.ok(procedimentoService.listarPaginado(tipo, trimestre, nome, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcedimentoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(procedimentoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcedimentoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CriarProcedimentoRequest request) {
        return ResponseEntity.ok(procedimentoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        procedimentoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
