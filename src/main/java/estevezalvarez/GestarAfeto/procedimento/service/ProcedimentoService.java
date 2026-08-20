package estevezalvarez.GestarAfeto.procedimento.service;

import estevezalvarez.GestarAfeto.procedimento.domain.ProcedimentoPreNatal;
import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;
import estevezalvarez.GestarAfeto.procedimento.domain.TrimestreGestacional;
import estevezalvarez.GestarAfeto.procedimento.dto.CriarProcedimentoRequest;
import estevezalvarez.GestarAfeto.procedimento.dto.ProcedimentoResponse;
import estevezalvarez.GestarAfeto.procedimento.repository.ProcedimentoRepository;
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
public class ProcedimentoService {

    private final ProcedimentoRepository procedimentoRepository;

    @Transactional
    public ProcedimentoResponse cadastrar(CriarProcedimentoRequest request) {
        validarSemanas(request.semanaInicialRecomendada(), request.semanaFinalRecomendada());
        ProcedimentoPreNatal procedimento = ProcedimentoPreNatal.builder()
            .nome(request.nome())
            .descricao(request.descricao())
            .tipo(request.tipo())
            .trimestreRecomendado(request.trimestreRecomendado())
            .semanaInicialRecomendada(request.semanaInicialRecomendada())
            .semanaFinalRecomendada(request.semanaFinalRecomendada())
            .obrigatorio(request.obrigatorio())
            .ativo(true)
            .build();
        return ProcedimentoResponse.from(procedimentoRepository.save(procedimento));
    }

    @Transactional(readOnly = true)
    public List<ProcedimentoResponse> listar() {
        return procedimentoRepository.findAll().stream()
            .map(ProcedimentoResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProcedimentoResponse> listarPaginado(
            TipoProcedimento tipo,
            TrimestreGestacional trimestre,
            String nome,
            Pageable pageable) {
        if (tipo != null) {
            return procedimentoRepository.findByTipoAndAtivoTrue(tipo, pageable).map(ProcedimentoResponse::from);
        }
        if (trimestre != null) {
            return procedimentoRepository.findByTrimestreRecomendadoAndAtivoTrue(trimestre, pageable)
                .map(ProcedimentoResponse::from);
        }
        if (nome != null && !nome.isBlank()) {
            return procedimentoRepository.findByNomeContainingIgnoreCaseAndAtivoTrue(nome, pageable)
                .map(ProcedimentoResponse::from);
        }
        return procedimentoRepository.findByAtivoTrue(pageable).map(ProcedimentoResponse::from);
    }

    @Transactional(readOnly = true)
    public ProcedimentoResponse buscarPorId(Long id) {
        return ProcedimentoResponse.from(buscarEntidade(id));
    }

    @Transactional
    public ProcedimentoResponse atualizar(Long id, CriarProcedimentoRequest request) {
        validarSemanas(request.semanaInicialRecomendada(), request.semanaFinalRecomendada());
        ProcedimentoPreNatal p = buscarEntidade(id);
        p.setNome(request.nome());
        p.setDescricao(request.descricao());
        p.setTipo(request.tipo());
        p.setTrimestreRecomendado(request.trimestreRecomendado());
        p.setSemanaInicialRecomendada(request.semanaInicialRecomendada());
        p.setSemanaFinalRecomendada(request.semanaFinalRecomendada());
        p.setObrigatorio(request.obrigatorio());
        return ProcedimentoResponse.from(procedimentoRepository.save(p));
    }

    @Transactional
    public void remover(Long id) {
        ProcedimentoPreNatal p = buscarEntidade(id);
        p.setAtivo(false);
        procedimentoRepository.save(p);
    }

    public ProcedimentoPreNatal buscarEntidade(Long id) {
        return procedimentoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Procedimento não encontrado com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProcedimentoPreNatal> listarAtivos() {
        return procedimentoRepository.findByAtivoTrueOrderByNomeAsc();
    }

    private void validarSemanas(Integer semanaInicial, Integer semanaFinal) {
        if (semanaInicial != null && (semanaInicial < 1 || semanaInicial > 42)) {
            throw new RegraDeNegocioException("Semana inicial recomendada deve estar entre 1 e 42.");
        }
        if (semanaFinal != null && (semanaFinal < 1 || semanaFinal > 42)) {
            throw new RegraDeNegocioException("Semana final recomendada deve estar entre 1 e 42.");
        }
        if (semanaInicial != null && semanaFinal != null && semanaInicial > semanaFinal) {
            throw new RegraDeNegocioException("Semana inicial recomendada deve ser menor ou igual a semana final.");
        }
    }
}
