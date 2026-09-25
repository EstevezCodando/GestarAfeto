package estevezalvarez.GestarAfeto.mensageria.config;

import estevezalvarez.GestarAfeto.mensageria.TopologiaEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infraestrutura de publicacao de eventos do servico principal.
 *
 * <p>O produtor declara <b>apenas o exchange</b>. Nao conhece filas, bindings nem
 * consumidores: publica em {@code gestarafeto.eventos} com uma routing key e segue. Quem
 * quiser receber declara a propria fila e se vincula ao exchange. E esse arranjo que
 * permite acrescentar um novo consumidor sem alterar uma linha deste servico.</p>
 */
@Configuration
public class RabbitProdutorConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitProdutorConfig.class);

    /** Durable para sobreviver ao reinicio do broker; nao autodelete para nao sumir sem consumidores. */
    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(TopologiaEventos.EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange eventosDlxExchange() {
        return new TopicExchange(TopologiaEventos.EXCHANGE_DLX, true, false);
    }

    /**
     * Conversor Jackson 3 ({@code JacksonJsonMessageConverter}). As mensagens trafegam em
     * JSON, legivel no painel do RabbitMQ e independente de linguagem, e nao em serializacao
     * binaria Java, que amarraria os dois lados as mesmas classes.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Publisher confirms ligados: o broker avisa se nao conseguiu aceitar a mensagem, e
     * returns avisa se ela nao pode ser roteada para nenhuma fila. Sem isso, uma routing key
     * errada descartaria o evento silenciosamente.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);

        template.setConfirmCallback((correlation, ack, causa) -> {
            if (!ack) {
                log.error("Broker nao confirmou a publicacao do evento {}: {}",
                    correlation == null ? "?" : correlation.getId(), causa);
            }
        });

        template.setReturnsCallback(retorno -> log.error(
            "Evento publicado mas nao roteado para nenhuma fila. routingKey={} motivo={}",
            retorno.getRoutingKey(), retorno.getReplyText()));

        return template;
    }
}
