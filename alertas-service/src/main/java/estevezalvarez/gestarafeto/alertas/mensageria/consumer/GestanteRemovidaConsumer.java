package estevezalvarez.gestarafeto.alertas.mensageria.consumer;

import estevezalvarez.gestarafeto.alertas.mensageria.TopologiaEventos;
import estevezalvarez.gestarafeto.alertas.mensageria.evento.GestanteRemovidaMessage;
import estevezalvarez.gestarafeto.alertas.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome {@code gestante.removida} e descarta os alertas da gestante.
 *
 * <p>E a contrapartida da ausencia de chave estrangeira entre os bancos: a limpeza dos dados
 * derivados e feita pela aplicacao, reagindo ao fato, e nao pelo banco em cascata.</p>
 *
 * <p>Tambem idempotente: remover alertas de uma gestante que ja nao tem nenhum apenas
 * devolve zero.</p>
 */
@Component
@RequiredArgsConstructor
public class GestanteRemovidaConsumer {

    private static final Logger log = LoggerFactory.getLogger(GestanteRemovidaConsumer.class);

    private final AlertaService alertaService;

    @RabbitListener(queues = TopologiaEventos.FILA_GESTANTE_REMOVIDA)
    public void consumir(GestanteRemovidaMessage mensagem) {
        if (mensagem == null || mensagem.gestanteId() == null) {
            throw new AmqpRejectAndDontRequeueException(
                "Evento de remocao sem gestanteId; enviando para a fila de dead letter.");
        }

        log.info("Evento recebido. tipo={} eventoId={} gestanteId={}",
            mensagem.tipo(), mensagem.eventoId(), mensagem.gestanteId());

        long removidos = alertaService.removerPorGestante(mensagem.gestanteId());

        log.info("Evento processado. eventoId={} gestanteId={} alertasRemovidos={}",
            mensagem.eventoId(), mensagem.gestanteId(), removidos);
    }
}
