package estevezalvarez.gestarafeto.alertas.mensageria.consumer;

import estevezalvarez.gestarafeto.alertas.dto.AvaliarChecklistRequest;
import estevezalvarez.gestarafeto.alertas.dto.ItemChecklistSnapshot;
import estevezalvarez.gestarafeto.alertas.mensageria.TopologiaEventos;
import estevezalvarez.gestarafeto.alertas.mensageria.evento.ChecklistAlteradoMessage;
import estevezalvarez.gestarafeto.alertas.mensageria.evento.ItemChecklistEvento;
import estevezalvarez.gestarafeto.alertas.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consome {@code checklist.alterado} e reavalia os alertas da gestante.
 *
 * <p>E aqui que o trabalho que antes acontecia dentro da requisicao HTTP do usuario passou a
 * acontecer: fora dela, em outro processo, no ritmo da fila.</p>
 *
 * <p><b>Idempotencia.</b> Processar a mesma mensagem duas vezes e inofensivo. A reconciliacao
 * de {@link AlertaService#avaliar} identifica cada alerta pelo par (item de origem, tipo) e
 * atualiza em vez de inserir. Isso importa porque o RabbitMQ garante entrega <i>ao menos uma
 * vez</i>: uma reentrega apos falha de rede nao pode duplicar alertas.</p>
 */
@Component
@RequiredArgsConstructor
public class ChecklistAlteradoConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChecklistAlteradoConsumer.class);

    private final AlertaService alertaService;

    @RabbitListener(queues = TopologiaEventos.FILA_CHECKLIST_ALTERADO)
    public void consumir(ChecklistAlteradoMessage mensagem) {
        validar(mensagem);

        log.info("Evento recebido. tipo={} eventoId={} gestanteId={} itens={}",
            mensagem.tipo(), mensagem.eventoId(), mensagem.gestanteId(),
            mensagem.itens() == null ? 0 : mensagem.itens().size());

        var resultado = alertaService.avaliar(new AvaliarChecklistRequest(
            mensagem.gestanteId(),
            mensagem.gestanteNome(),
            mensagem.dataUltimaMenstruacao(),
            mensagem.dataProvavelParto(),
            converter(mensagem.itens())));

        log.info("Evento processado. eventoId={} gestanteId={} alertasAtivos={}",
            mensagem.eventoId(), mensagem.gestanteId(), resultado.size());
    }

    /**
     * Mensagem malformada nao e problema transitorio: repetir nao vai corrigi-la. Rejeitar
     * sem reenfileirar manda direto para a DLQ, onde pode ser inspecionada, em vez de ocupar
     * as tentativas de repeticao e atrasar as mensagens seguintes da fila.
     */
    private void validar(ChecklistAlteradoMessage mensagem) {
        if (mensagem == null || mensagem.gestanteId() == null) {
            throw new AmqpRejectAndDontRequeueException(
                "Evento de checklist sem gestanteId; enviando para a fila de dead letter.");
        }
        if (mensagem.gestanteNome() == null || mensagem.gestanteNome().isBlank()) {
            throw new AmqpRejectAndDontRequeueException(
                "Evento de checklist sem nome da gestante; enviando para a fila de dead letter.");
        }
    }

    private List<ItemChecklistSnapshot> converter(List<ItemChecklistEvento> itens) {
        if (itens == null) {
            return List.of();
        }
        return itens.stream()
            .map(i -> new ItemChecklistSnapshot(
                i.itemId(),
                i.procedimentoNome(),
                i.procedimentoTipo(),
                i.status(),
                i.obrigatorio(),
                i.semanaInicialRecomendada(),
                i.semanaFinalRecomendada(),
                i.dataRealizacao()))
            .toList();
    }
}
