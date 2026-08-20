package estevezalvarez.GestarAfeto.consulta.service;

import estevezalvarez.GestarAfeto.consulta.domain.ConsultaPreNatal;
import estevezalvarez.GestarAfeto.consulta.dto.ConsultaResponse;
import estevezalvarez.GestarAfeto.consulta.dto.CriarConsultaRequest;
import estevezalvarez.GestarAfeto.consulta.repository.ConsultaPreNatalRepository;
import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import estevezalvarez.GestarAfeto.gestante.service.GestanteService;
import estevezalvarez.GestarAfeto.shared.exception.RecursoNaoEncontradoException;
import estevezalvarez.GestarAfeto.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultaService {

    private final ConsultaPreNatalRepository consultaRepository;
    private final GestanteService gestanteService;

    @Transactional
    public ConsultaResponse registrar(Long gestanteId, CriarConsultaRequest request) {
        validarConsulta(request);
        Gestante gestante = gestanteService.buscarEntidade(gestanteId);
        ConsultaPreNatal consulta = ConsultaPreNatal.builder()
            .gestante(gestante)
            .dataConsulta(request.dataConsulta())
            .semanaGestacional(request.semanaGestacional())
            .peso(request.peso())
            .pressaoArterial(request.pressaoArterial())
            .observacoes(request.observacoes())
            .build();
        return ConsultaResponse.from(consultaRepository.save(consulta));
    }

    @Transactional(readOnly = true)
    public List<ConsultaResponse> listarPorGestante(Long gestanteId) {
        gestanteService.buscarEntidade(gestanteId);
        return consultaRepository.findByGestanteIdOrderByDataConsultaDesc(gestanteId).stream()
            .map(ConsultaResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<ConsultaResponse> listarPorGestantePaginado(Long gestanteId, Pageable pageable) {
        gestanteService.buscarEntidade(gestanteId);
        return consultaRepository.findByGestanteIdOrderByDataConsultaDesc(gestanteId, pageable)
            .map(ConsultaResponse::from);
    }

    @Transactional(readOnly = true)
    public ConsultaResponse buscarPorId(Long id) {
        return ConsultaResponse.from(buscarEntidade(id));
    }

    @Transactional
    public ConsultaResponse atualizar(Long id, CriarConsultaRequest request) {
        validarConsulta(request);
        ConsultaPreNatal consulta = buscarEntidade(id);
        consulta.setDataConsulta(request.dataConsulta());
        consulta.setSemanaGestacional(request.semanaGestacional());
        consulta.setPeso(request.peso());
        consulta.setPressaoArterial(request.pressaoArterial());
        consulta.setObservacoes(request.observacoes());
        return ConsultaResponse.from(consultaRepository.save(consulta));
    }

    @Transactional
    public void remover(Long id) {
        buscarEntidade(id);
        consultaRepository.deleteById(id);
    }

    private ConsultaPreNatal buscarEntidade(Long id) {
        return consultaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta nao encontrada com id: " + id));
    }

    private void validarConsulta(CriarConsultaRequest request) {
        if (request.semanaGestacional() != null && (request.semanaGestacional() < 1 || request.semanaGestacional() > 42)) {
            throw new RegraDeNegocioException("Semana gestacional deve estar entre 1 e 42.");
        }
        if (request.peso() != null && request.peso().signum() <= 0) {
            throw new RegraDeNegocioException("Peso deve ser maior que zero.");
        }
    }
}
