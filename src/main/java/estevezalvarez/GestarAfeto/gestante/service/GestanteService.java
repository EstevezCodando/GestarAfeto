package estevezalvarez.GestarAfeto.gestante.service;

import estevezalvarez.GestarAfeto.checklist.repository.ItemChecklistGestanteRepository;
import estevezalvarez.GestarAfeto.consulta.repository.ConsultaPreNatalRepository;
import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import estevezalvarez.GestarAfeto.gestante.dto.CriarGestanteRequest;
import estevezalvarez.GestarAfeto.gestante.dto.GestanteResponse;
import estevezalvarez.GestarAfeto.gestante.repository.GestanteRepository;
import estevezalvarez.GestarAfeto.shared.event.GestanteRemovidaEvent;
import estevezalvarez.GestarAfeto.shared.exception.RecursoNaoEncontradoException;
import estevezalvarez.GestarAfeto.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GestanteService {

    private final GestanteRepository gestanteRepository;
    private final ItemChecklistGestanteRepository itemChecklistRepository;
    private final ConsultaPreNatalRepository consultaRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public GestanteResponse cadastrar(CriarGestanteRequest request) {
        if (request.dataProvavelParto() == null && request.dataUltimaMenstruacao() == null) {
            throw new RegraDeNegocioException("Informe a data provável do parto ou a data da última menstruação.");
        }
        Gestante gestante = Gestante.builder()
            .nome(request.nome())
            .dataNascimento(request.dataNascimento())
            .telefone(request.telefone())
            .email(request.email())
            .dataUltimaMenstruacao(request.dataUltimaMenstruacao())
            .dataProvavelParto(request.dataProvavelParto())
            .observacoes(request.observacoes())
            .build();
        return GestanteResponse.from(gestanteRepository.save(gestante));
    }

    @Transactional(readOnly = true)
    public List<GestanteResponse> listar() {
        return gestanteRepository.findAll().stream()
            .map(GestanteResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<GestanteResponse> listarPaginado(String nome, Pageable pageable) {
        Page<Gestante> pagina = nome == null || nome.isBlank()
            ? gestanteRepository.findAll(pageable)
            : gestanteRepository.findByNomeContainingIgnoreCase(nome, pageable);
        return pagina.map(GestanteResponse::from);
    }

    @Transactional(readOnly = true)
    public GestanteResponse buscarPorId(Long id) {
        return GestanteResponse.from(buscarEntidade(id));
    }

    @Transactional
    public GestanteResponse atualizar(Long id, CriarGestanteRequest request) {
        if (request.dataProvavelParto() == null && request.dataUltimaMenstruacao() == null) {
            throw new RegraDeNegocioException("Informe a data provavel do parto ou a data da ultima menstruacao.");
        }
        Gestante gestante = buscarEntidade(id);
        gestante.setNome(request.nome());
        gestante.setDataNascimento(request.dataNascimento());
        gestante.setTelefone(request.telefone());
        gestante.setEmail(request.email());
        gestante.setDataUltimaMenstruacao(request.dataUltimaMenstruacao());
        gestante.setDataProvavelParto(request.dataProvavelParto());
        gestante.setObservacoes(request.observacoes());
        return GestanteResponse.from(gestanteRepository.save(gestante));
    }

    @Transactional
    public void remover(Long id) {
        buscarEntidade(id);
        itemChecklistRepository.deleteByGestanteId(id);
        consultaRepository.deleteByGestanteId(id);
        gestanteRepository.deleteById(id);
        // Dados derivados mantidos por outros servicos (como os alertas) sao descartados
        // por quem escuta este evento, apos o commit da remocao.
        eventPublisher.publishEvent(new GestanteRemovidaEvent(id));
    }

    public Gestante buscarEntidade(Long id) {
        return gestanteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Gestante não encontrada com id: " + id));
    }
}
