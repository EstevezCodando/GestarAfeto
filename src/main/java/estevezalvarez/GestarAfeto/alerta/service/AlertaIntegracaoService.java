package estevezalvarez.GestarAfeto.alerta.service;

import estevezalvarez.GestarAfeto.alerta.client.AlertaClient;
import estevezalvarez.GestarAfeto.alerta.client.dto.AlertaResponse;
import estevezalvarez.GestarAfeto.alerta.client.dto.AvaliarChecklistRequest;
import estevezalvarez.GestarAfeto.alerta.client.dto.ItemChecklistSnapshot;
import estevezalvarez.GestarAfeto.alerta.client.dto.ResumoAlertasResponse;
import estevezalvarez.GestarAfeto.checklist.domain.ItemChecklistGestante;
import estevezalvarez.GestarAfeto.checklist.repository.ItemChecklistGestanteRepository;
import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import estevezalvarez.GestarAfeto.gestante.service.GestanteService;
import estevezalvarez.GestarAfeto.procedimento.domain.ProcedimentoPreNatal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ponte entre o dominio do servico principal e o microsservico de alertas.
 *
 * <p>Traduz gestante + itens de checklist para o contrato do microsservico e delega ao
 * {@link AlertaClient}. Nenhum metodo aqui e {@code @Transactional}: a leitura dos dados
 * acontece em transacoes curtas dos proprios repositories e a chamada remota fica fora
 * de qualquer transacao aberta, evitando segurar conexao de banco durante I/O de rede.</p>
 */
@Service
@RequiredArgsConstructor
public class AlertaIntegracaoService {

    private static final Logger log = LoggerFactory.getLogger(AlertaIntegracaoService.class);

    private final AlertaClient alertaClient;
    private final GestanteService gestanteService;
    private final ItemChecklistGestanteRepository itemChecklistRepository;

    /**
     * Reavalia o checklist da gestante no microsservico e devolve os alertas ativos.
     *
     * @throws estevezalvarez.GestarAfeto.shared.exception.RecursoNaoEncontradoException
     *         se a gestante nao existir
     */
    public List<AlertaResponse> reavaliar(Long gestanteId) {
        Gestante gestante = gestanteService.buscarEntidade(gestanteId);
        // @EntityGraph carrega o procedimento junto, entao as janelas recomendadas estao
        // disponiveis sem consulta adicional e sem depender de sessao aberta.
        List<ItemChecklistGestante> itens = itemChecklistRepository.findByGestanteId(gestanteId);

        AvaliarChecklistRequest request = new AvaliarChecklistRequest(
            gestante.getId(),
            gestante.getNome(),
            gestante.getDataUltimaMenstruacao(),
            gestante.getDataProvavelParto(),
            itens.stream().map(this::snapshot).toList());

        return alertaClient.avaliar(request);
    }

    /**
     * Variante usada por gatilhos automaticos: nunca propaga erro.
     *
     * <p>Uma falha ao sincronizar alertas nao pode invalidar a operacao de checklist que
     * o usuario acabou de concluir com sucesso.</p>
     */
    public void reavaliarSemPropagarErro(Long gestanteId) {
        try {
            reavaliar(gestanteId);
        } catch (RuntimeException ex) {
            log.warn("Falha ao sincronizar alertas da gestante {}: {}", gestanteId, ex.getMessage());
        }
    }

    public List<AlertaResponse> listar(Long gestanteId) {
        gestanteService.buscarEntidade(gestanteId);
        return alertaClient.listarPorGestante(gestanteId);
    }

    public ResumoAlertasResponse resumo(Long gestanteId) {
        gestanteService.buscarEntidade(gestanteId);
        return alertaClient.resumo(gestanteId);
    }

    public AlertaResponse marcarLido(Long alertaId) {
        return alertaClient.marcarLido(alertaId);
    }

    public AlertaResponse resolver(Long alertaId) {
        return alertaClient.resolver(alertaId);
    }

    public void removerPorGestante(Long gestanteId) {
        alertaClient.removerPorGestante(gestanteId);
    }

    private ItemChecklistSnapshot snapshot(ItemChecklistGestante item) {
        ProcedimentoPreNatal procedimento = item.getProcedimento();
        return new ItemChecklistSnapshot(
            item.getId(),
            procedimento.getNome(),
            procedimento.getTipo() == null ? null : procedimento.getTipo().name(),
            item.getStatus() == null ? null : item.getStatus().name(),
            procedimento.isObrigatorio(),
            procedimento.getSemanaInicialRecomendada(),
            procedimento.getSemanaFinalRecomendada(),
            item.getDataRealizacao());
    }
}
