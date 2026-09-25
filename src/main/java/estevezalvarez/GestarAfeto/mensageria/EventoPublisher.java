package estevezalvarez.GestarAfeto.mensageria;

import estevezalvarez.GestarAfeto.mensageria.evento.ChecklistAlteradoMessage;
import estevezalvarez.GestarAfeto.mensageria.evento.GestanteRemovidaMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Unico ponto do servico principal que fala com o RabbitMQ.
 *
 * <p>Concentrar a publicacao aqui mantem o cabecalho das mensagens padronizado e deixa um
 * lugar so para evoluir rastreamento e tratamento de falha.</p>
 *
 * <p><b>Falha na publicacao nao propaga.</b> O evento e publicado depois do commit da
 * operacao de dominio; se o broker estiver fora do ar nesse instante, a operacao do usuario
 * ja foi concluida com sucesso e nao pode ser transformada em erro. A mensagem e perdida e o
 * estado se recompoe na proxima alteracao do checklist ou numa reavaliacao manual, porque o
 * processamento no consumidor e idempotente. Eliminar essa janela exigiria um outbox
 * transacional, registrado como evolucao na documentacao.</p>
 */
@Component
@RequiredArgsConstructor
public class EventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventoPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publicarChecklistAlterado(ChecklistAlteradoMessage mensagem) {
        publicar(TopologiaEventos.RK_CHECKLIST_ALTERADO, mensagem, mensagem.eventoId(),
            mensagem.tipo(), mensagem.gestanteId());
    }

    public void publicarGestanteRemovida(GestanteRemovidaMessage mensagem) {
        publicar(TopologiaEventos.RK_GESTANTE_REMOVIDA, mensagem, mensagem.eventoId(),
            mensagem.tipo(), mensagem.gestanteId());
    }

    private void publicar(String routingKey, Object mensagem, String eventoId,
                          String tipo, Long gestanteId) {
        try {
            rabbitTemplate.convertAndSend(
                TopologiaEventos.EXCHANGE, routingKey, mensagem,
                new CorrelationData(eventoId));
            log.info("Evento publicado. tipo={} eventoId={} gestanteId={} routingKey={}",
                tipo, eventoId, gestanteId, routingKey);
        } catch (AmqpException ex) {
            log.error("Falha ao publicar evento. tipo={} eventoId={} gestanteId={} causa={}",
                tipo, eventoId, gestanteId, ex.getMessage());
        }
    }
}
