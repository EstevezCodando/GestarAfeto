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
 * Mantem os alertas sincronizados automaticamente com o servico principal.
 *
 * <p>Roda em {@link TransactionPhase#AFTER_COMMIT}: o microsservico so e chamado depois que a
 * alteracao esta efetivamente gravada, evitando avaliar um estado que a transacao ainda
 * poderia desfazer. As chamadas tambem nao propagam erro, entao o microsservico fora do ar
 * nunca transforma uma operacao concluida com sucesso em erro para a usuaria.</p>
 *
 * <p>Este listener e o unico ponto de acoplamento entre os dominios de checklist/gestante e o
 * de alertas: os publicadores conhecem apenas o evento em {@code shared.event}.</p>
 *
 * <p>Pode ser desligado com {@code gestarafeto.alertas.integracao-automatica=false}, util em
 * testes e em ambientes onde o microsservico nao esta implantado.</p>
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
        alertaIntegracaoService.reavaliarSemPropagarErro(evento.gestanteId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoRemoverGestante(GestanteRemovidaEvent evento) {
        try {
            alertaIntegracaoService.removerPorGestante(evento.gestanteId());
        } catch (RuntimeException ex) {
            log.warn("Falha ao remover alertas da gestante {}: {}", evento.gestanteId(), ex.getMessage());
        }
    }
}
