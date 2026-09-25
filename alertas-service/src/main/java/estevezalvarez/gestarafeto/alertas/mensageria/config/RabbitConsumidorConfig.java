package estevezalvarez.gestarafeto.alertas.mensageria.config;

import estevezalvarez.gestarafeto.alertas.mensageria.TopologiaEventos;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia e infraestrutura de consumo do microsservico de alertas.
 *
 * <p>Quem declara filas e bindings e o <b>consumidor</b>, nao o produtor. O servico
 * principal publica em {@code gestarafeto.eventos} sem saber que estas filas existem; este
 * servico se inscreve no que lhe interessa. Um terceiro servico poderia se vincular ao mesmo
 * exchange com as proprias filas, e nada no produtor mudaria.</p>
 *
 * <p>Toda a topologia e declarada em codigo e criada automaticamente na subida
 * ({@code RabbitAdmin}). Nao ha passo manual de configuracao no broker, e o ambiente e
 * reproduzivel do zero.</p>
 */
@Configuration
public class RabbitConsumidorConfig {

    // ------------------------------------------------------------------------ exchanges

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(TopologiaEventos.EXCHANGE, true, false);
    }

    /**
     * Exchange de dead letter. As filas principais apontam para ele; quando uma mensagem
     * esgota as tentativas, o broker a reencaminha para ca com a mesma routing key.
     */
    @Bean
    public TopicExchange eventosDlxExchange() {
        return new TopicExchange(TopologiaEventos.EXCHANGE_DLX, true, false);
    }

    // ------------------------------------------------------------- filas e dead lettering

    @Bean
    public Queue filaChecklistAlterado() {
        return QueueBuilder.durable(TopologiaEventos.FILA_CHECKLIST_ALTERADO)
            .deadLetterExchange(TopologiaEventos.EXCHANGE_DLX)
            .deadLetterRoutingKey(TopologiaEventos.RK_CHECKLIST_ALTERADO)
            .build();
    }

    @Bean
    public Queue filaGestanteRemovida() {
        return QueueBuilder.durable(TopologiaEventos.FILA_GESTANTE_REMOVIDA)
            .deadLetterExchange(TopologiaEventos.EXCHANGE_DLX)
            .deadLetterRoutingKey(TopologiaEventos.RK_GESTANTE_REMOVIDA)
            .build();
    }

    @Bean
    public Queue filaChecklistAlteradoDlq() {
        return QueueBuilder.durable(TopologiaEventos.FILA_CHECKLIST_ALTERADO_DLQ).build();
    }

    @Bean
    public Queue filaGestanteRemovidaDlq() {
        return QueueBuilder.durable(TopologiaEventos.FILA_GESTANTE_REMOVIDA_DLQ).build();
    }

    // -------------------------------------------------------------------------- bindings

    @Bean
    public Binding bindingChecklistAlterado(Queue filaChecklistAlterado,
                                            TopicExchange eventosExchange) {
        return BindingBuilder.bind(filaChecklistAlterado)
            .to(eventosExchange)
            .with(TopologiaEventos.RK_CHECKLIST_ALTERADO);
    }

    @Bean
    public Binding bindingGestanteRemovida(Queue filaGestanteRemovida,
                                           TopicExchange eventosExchange) {
        return BindingBuilder.bind(filaGestanteRemovida)
            .to(eventosExchange)
            .with(TopologiaEventos.RK_GESTANTE_REMOVIDA);
    }

    @Bean
    public Binding bindingChecklistAlteradoDlq(Queue filaChecklistAlteradoDlq,
                                               TopicExchange eventosDlxExchange) {
        return BindingBuilder.bind(filaChecklistAlteradoDlq)
            .to(eventosDlxExchange)
            .with(TopologiaEventos.RK_CHECKLIST_ALTERADO);
    }

    @Bean
    public Binding bindingGestanteRemovidaDlq(Queue filaGestanteRemovidaDlq,
                                              TopicExchange eventosDlxExchange) {
        return BindingBuilder.bind(filaGestanteRemovidaDlq)
            .to(eventosDlxExchange)
            .with(TopologiaEventos.RK_GESTANTE_REMOVIDA);
    }

    // ------------------------------------------------------------------------ conversao

    /**
     * Conversor JSON com precedencia de tipo <b>INFERRED</b>.
     *
     * <p>Isso e essencial aqui. Por padrao o Jackson do Spring AMQP usa o cabecalho
     * {@code __TypeId__} que o produtor envia, que carrega o nome da classe Java <i>dele</i>
     * ({@code estevezalvarez.GestarAfeto.mensageria.evento...}). Essa classe nao existe neste
     * servico, e a desserializacao falharia.</p>
     *
     * <p>Com {@code INFERRED}, o cabecalho e ignorado e o alvo passa a ser o tipo do
     * parametro do metodo anotado com {@code @RabbitListener}. Os dois lados ficam livres
     * para nomear e empacotar suas classes como quiserem: o contrato e o JSON, nao a classe.</p>
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper mapper = new DefaultJacksonJavaTypeMapper();
        mapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(mapper);
        return converter;
    }
}
