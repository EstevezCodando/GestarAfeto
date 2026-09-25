package estevezalvarez.GestarAfeto.alerta.service;

import estevezalvarez.GestarAfeto.alerta.client.AlertaClient;
import estevezalvarez.GestarAfeto.alerta.client.dto.AlertaResponse;
import estevezalvarez.GestarAfeto.alerta.client.dto.ResumoAlertasResponse;
import estevezalvarez.GestarAfeto.checklist.domain.ItemChecklistGestante;
import estevezalvarez.GestarAfeto.checklist.repository.ItemChecklistGestanteRepository;
import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import estevezalvarez.GestarAfeto.gestante.service.GestanteService;
import estevezalvarez.GestarAfeto.mensageria.EventoPublisher;
import estevezalvarez.GestarAfeto.mensageria.evento.ChecklistAlteradoMessage;
import estevezalvarez.GestarAfeto.mensageria.evento.GestanteRemovidaMessage;
import estevezalvarez.GestarAfeto.mensageria.evento.ItemChecklistEvento;
import estevezalvarez.GestarAfeto.procedimento.domain.ProcedimentoPreNatal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ponte entre o dominio do servico principal e o de alertas.
 *
 * <p>A integracao usa dois canais, escolhidos por caso de uso:</p>
 *
 * <ul>
 *   <li><b>Escrita, por evento (RabbitMQ).</b> Mudou o checklist ou a gestante foi removida?
 *       O servico publica o fato e segue. Nao espera resposta, nao sabe quem processa e nao
 *       depende de o consumidor estar no ar naquele instante.</li>
 *   <li><b>Leitura, por REST (Feign).</b> Listar alertas e montar o resumo sao consultas
 *       sincronas por natureza: a tela precisa da resposta agora. Transformar isso em evento
 *       traria complexidade sem beneficio, entao permanece requisicao e resposta.</li>
 * </ul>
 *
 * <p>Nenhum metodo e {@code @Transactional}: a leitura acontece em transacoes curtas dos
 * proprios repositories, e a publicacao fica fora de qualquer transacao aberta.</p>
 */
@Service
@RequiredArgsConstructor
public class AlertaIntegracaoService {

    private final AlertaClient alertaClient;
    private final EventoPublisher eventoPublisher;
    private final GestanteService gestanteService;
    private final ItemChecklistGestanteRepository itemChecklistRepository;

    // ------------------------------------------------------------ escrita (assincrona)

    /**
     * Publica o estado atual do checklist para reavaliacao assincrona dos alertas.
     *
     * <p>Retorna assim que a mensagem e entregue ao broker. O resultado do processamento
     * aparece nas consultas seguintes, nao nesta chamada.</p>
     *
     * @throws estevezalvarez.GestarAfeto.shared.exception.RecursoNaoEncontradoException
     *         se a gestante nao existir
     */
    public void solicitarReavaliacao(Long gestanteId) {
        Gestante gestante = gestanteService.buscarEntidade(gestanteId);
        // @EntityGraph carrega o procedimento junto, entao as janelas recomendadas estao
        // disponiveis sem consulta adicional e sem depender de sessao aberta.
        List<ItemChecklistGestante> itens = itemChecklistRepository.findByGestanteId(gestanteId);

        eventoPublisher.publicarChecklistAlterado(ChecklistAlteradoMessage.nova(
            gestante.getId(),
            gestante.getNome(),
            gestante.getDataUltimaMenstruacao(),
            gestante.getDataProvavelParto(),
            itens.stream().map(this::snapshot).toList()));
    }

    /** Anuncia a remocao da gestante para que os dados derivados sejam descartados. */
    public void solicitarRemocaoDosAlertas(Long gestanteId) {
        eventoPublisher.publicarGestanteRemovida(GestanteRemovidaMessage.nova(gestanteId));
    }

    // ------------------------------------------------------------- leitura (sincrona)

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

    // ------------------------------------------------------------------------ interno

    private ItemChecklistEvento snapshot(ItemChecklistGestante item) {
        ProcedimentoPreNatal procedimento = item.getProcedimento();
        return new ItemChecklistEvento(
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
