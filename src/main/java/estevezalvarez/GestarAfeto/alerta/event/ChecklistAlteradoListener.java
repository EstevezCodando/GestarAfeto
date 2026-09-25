package estevezalvarez.GestarAfeto.alerta.event;

import estevezalvarez.GestarAfeto.alerta.service.AlertaIntegracaoService;
import estevezalvarez.GestarAfeto.shared.event.ChecklistAlteradoEvent;
import estevezalvarez.GestarAfeto.shared.event.GestanteRemovidaEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Converte eventos de dominio internos em mensagens publicadas no RabbitMQ.
 *
 * <p>Fica na fronteira entre dois mundos. Para dentro, escuta os eventos que
 * {@code ChecklistService} e {@code GestanteService} publicam no contexto do Spring, sem que
 * esses servicos saibam que existe mensageria. Para fora, traduz o fato em uma mensagem e a
 * entrega ao broker.</p>
 *
 * <p>Roda em {@link TransactionPhase#AFTER_COMMIT}, e isso importa: so e publicado o que
 * realmente foi gravado. Publicar antes do commit poderia anunciar uma alteracao que a
 * transacao ainda desfaria, e o consumidor processaria um estado que nunca existiu.</p>
 *
 * <p>Comparado ao TP3, o que mudou aqui e a natureza da chamada. Antes este ponto fazia uma
 * requisicao HTTP e ficava bloqueado esperando o microsservico responder; agora entrega a
 * mensagem ao broker e retorna. O produtor deixou de depender da execucao imediata do
 * consumidor para concluir sua propria operacao.</p>
 *
 * <p>Pode ser desligado com {@code gestarafeto.alertas.integracao-automatica=false}, util em
 * testes e em ambientes sem broker.</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "gestarafeto.alertas.integracao-automatica",
    havingValue = "true",
    matchIfMissing = true)
public class ChecklistAlteradoListener {

    private static final Logger log = LoggerFactory.getLogger(ChecklistAlteradoListener.class);

    private final AlertaIntegracaoService alertaIntegracaoService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoAlterarChecklist(ChecklistAlteradoEvent evento) {
        try {
            alertaIntegracaoService.solicitarReavaliacao(evento.gestanteId());
        } catch (RuntimeException ex) {
            // Publicar e um efeito colateral da operacao que ja foi concluida com sucesso.
            // Falhar aqui nao pode invalidar o que o usuario acabou de salvar.
            log.warn("Nao foi possivel anunciar a alteracao do checklist da gestante {}: {}",
                evento.gestanteId(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoRemoverGestante(GestanteRemovidaEvent evento) {
        try {
            alertaIntegracaoService.solicitarRemocaoDosAlertas(evento.gestanteId());
        } catch (RuntimeException ex) {
            log.warn("Nao foi possivel anunciar a remocao da gestante {}: {}",
                evento.gestanteId(), ex.getMessage());
        }
    }
}
