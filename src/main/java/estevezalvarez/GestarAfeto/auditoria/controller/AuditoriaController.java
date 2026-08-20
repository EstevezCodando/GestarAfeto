package estevezalvarez.GestarAfeto.auditoria.controller;

import estevezalvarez.GestarAfeto.auditoria.dto.AuditoriaRevisionResponse;
import estevezalvarez.GestarAfeto.auditoria.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping("/{entidade}/{id}")
    public ResponseEntity<List<AuditoriaRevisionResponse>> historico(
            @PathVariable String entidade,
            @PathVariable Long id) {
        return ResponseEntity.ok(auditoriaService.historico(entidade, id));
    }
}
