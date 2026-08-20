package estevezalvarez.GestarAfeto.checklist.service;

import estevezalvarez.GestarAfeto.checklist.domain.ItemChecklistGestante;
import estevezalvarez.GestarAfeto.checklist.domain.StatusChecklist;
import estevezalvarez.GestarAfeto.checklist.dto.AtualizarStatusChecklistRequest;
import estevezalvarez.GestarAfeto.checklist.dto.ItemChecklistResponse;
import estevezalvarez.GestarAfeto.checklist.repository.ItemChecklistGestanteRepository;
import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import estevezalvarez.GestarAfeto.gestante.service.GestanteService;
import estevezalvarez.GestarAfeto.procedimento.domain.ProcedimentoPreNatal;
import estevezalvarez.GestarAfeto.procedimento.service.ProcedimentoService;
import estevezalvarez.GestarAfeto.shared.event.ChecklistAlteradoEvent;
import estevezalvarez.GestarAfeto.shared.exception.RecursoNaoEncontradoException;
import estevezalvarez.GestarAfeto.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ItemChecklistGestanteRepository itemRepository;
    private final GestanteService gestanteService;
    private final ProcedimentoService procedimentoService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public List<ItemChecklistResponse> gerarChecklist(Long gestanteId) {
        if (itemRepository.existsByGestanteId(gestanteId)) {
            throw new RegraDeNegocioException("Checklist ja gerado para esta gestante.");
        }
        Gestante gestante = gestanteService.buscarEntidade(gestanteId);
        List<ProcedimentoPreNatal> procedimentos = procedimentoService.listarAtivos();

        List<ItemChecklistGestante> itens = procedimentos.stream()
            .map(proc -> ItemChecklistGestante.builder()
                .gestante(gestante)
                .procedimento(proc)
                .status(StatusChecklist.PENDENTE)
                .build())
            .toList();

        List<ItemChecklistResponse> criados = itemRepository.saveAll(itens).stream()
            .map(ItemChecklistResponse::from)
            .toList();
        eventPublisher.publishEvent(new ChecklistAlteradoEvent(gestanteId));
        return criados;
    }

    @Transactional(readOnly = true)
    public List<ItemChecklistResponse> listarPorGestante(Long gestanteId) {
        return listarPorGestante(gestanteId, null);
    }

    @Transactional(readOnly = true)
    public List<ItemChecklistResponse> listarPorGestante(Long gestanteId, StatusChecklist status) {
        gestanteService.buscarEntidade(gestanteId);
        List<ItemChecklistGestante> itens = status == null
            ? itemRepository.findByGestanteId(gestanteId)
            : itemRepository.findByGestanteIdAndStatus(gestanteId, status);
        return itens.stream()
            .map(ItemChecklistResponse::from)
            .toList();
    }

    @Transactional
    public ItemChecklistResponse atualizarStatus(Long itemId, AtualizarStatusChecklistRequest request) {
        ItemChecklistGestante item = buscarItem(itemId);
        if (request.status() == StatusChecklist.REALIZADO && request.dataRealizacao() == null) {
            throw new RegraDeNegocioException("Data de realizacao e obrigatoria para status REALIZADO.");
        }
        if (request.status() != StatusChecklist.REALIZADO && request.dataRealizacao() != null) {
            throw new RegraDeNegocioException("Data de realizacao deve ser informada apenas para status REALIZADO.");
        }
        item.setStatus(request.status());
        item.setDataRealizacao(request.dataRealizacao());
        item.setObservacao(request.observacao());
        return salvarEnotificar(item);
    }

    @Transactional
    public ItemChecklistResponse marcarRealizado(Long itemId) {
        ItemChecklistGestante item = buscarItem(itemId);
        item.setStatus(StatusChecklist.REALIZADO);
        item.setDataRealizacao(LocalDate.now());
        return salvarEnotificar(item);
    }

    @Transactional
    public ItemChecklistResponse marcarParaRevisar(Long itemId) {
        ItemChecklistGestante item = buscarItem(itemId);
        item.setStatus(StatusChecklist.PRECISA_REVISAR);
        item.setDataRealizacao(null);
        return salvarEnotificar(item);
    }

    /**
     * Persiste a alteracao e anuncia que o checklist mudou. O evento e entregue apenas apos
     * o commit, entao quem reage a ele enxerga o estado ja gravado.
     */
    private ItemChecklistResponse salvarEnotificar(ItemChecklistGestante item) {
        ItemChecklistResponse salvo = ItemChecklistResponse.from(itemRepository.save(item));
        eventPublisher.publishEvent(new ChecklistAlteradoEvent(item.getGestante().getId()));
        return salvo;
    }

    private ItemChecklistGestante buscarItem(Long itemId) {
        return itemRepository.findById(itemId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Item de checklist nao encontrado com id: " + itemId));
    }
}
