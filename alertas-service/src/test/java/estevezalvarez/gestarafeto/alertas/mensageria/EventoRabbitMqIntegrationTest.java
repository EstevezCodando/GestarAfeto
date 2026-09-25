package estevezalvarez.gestarafeto.alertas.mensageria;

import estevezalvarez.gestarafeto.alertas.repository.AlertaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Percorre o caminho completo da mensagem com um RabbitMQ de verdade: publicacao no
 * exchange, roteamento pela routing key, entrega na fila e consumo pelo listener.
 *
 * <p>Os demais testes chamam o metodo do consumidor diretamente, o que valida a regra mas
 * nao o transporte. Aqui a mensagem e publicada como JSON no broker e o teste so olha o
 * banco: se o alerta aparecer, e porque exchange, binding, fila, conversao e listener
 * funcionaram de ponta a ponta.</p>
 *
 * <p>A mensagem e publicada com um {@code RabbitTemplate} configurado neste teste, sem
 * nenhuma classe do servico produtor no classpath. Isso prova que o contrato entre os dois
 * lados e o <b>JSON</b>, e nao uma classe Java compartilhada.</p>
 *
 * <p>Como os demais testes de infraestrutura do projeto, e ignorado automaticamente quando
 * nao ha Docker disponivel.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
    // O profile de teste desliga a mensageria; aqui ela e religada para valer.
    "spring.rabbitmq.dynamic=true",
    "spring.rabbitmq.listener.simple.auto-startup=true"
})
class EventoRabbitMqIntegrationTest {

    private static final Long GESTANTE_ID = 700L;

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private AlertaRepository alertaRepository;

    @BeforeEach
    void limpar() {
        alertaRepository.deleteAll();
    }

    /**
     * Mensagem montada como mapa, e nao como classe do produtor: o consumidor precisa
     * entender o JSON, venha ele de onde vier.
     */
    private Map<String, Object> eventoChecklist(String statusItem) {
        return Map.of(
            "eventoId", UUID.randomUUID().toString(),
            "tipo", "CHECKLIST_ALTERADO",
            "versao", 1,
            "ocorridoEm", Instant.now().toString(),
            "origem", "gestarafeto-principal",
            "gestanteId", GESTANTE_ID,
            "gestanteNome", "Maria Silva",
            "dataUltimaMenstruacao", LocalDate.now().minusWeeks(25).toString(),
            "itens", List.of(Map.of(
                "itemId", 10,
                "procedimentoNome", "Hemograma Completo",
                "procedimentoTipo", "EXAME",
                "status", statusItem,
                "obrigatorio", true,
                "semanaInicialRecomendada", 1,
                "semanaFinalRecomendada", 13)));
    }

    private void aguardar(String oQue, BooleanSupplier condicao) {
        long limite = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
        while (System.currentTimeMillis() < limite) {
            if (condicao.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("Tempo esgotado aguardando: " + oQue);
    }

    @Test
    void topologiaEDeclaradaNoBroker() {
        // Filas e bindings sao criados pelo proprio servico na subida, sem passo manual.
        for (String fila : List.of(
                TopologiaEventos.FILA_CHECKLIST_ALTERADO,
                TopologiaEventos.FILA_GESTANTE_REMOVIDA,
                TopologiaEventos.FILA_CHECKLIST_ALTERADO_DLQ,
                TopologiaEventos.FILA_GESTANTE_REMOVIDA_DLQ)) {
            QueueInformation info = ((RabbitAdmin) amqpAdmin).getQueueInfo(fila);
            assertNotNull(info, "fila nao declarada no broker: " + fila);
        }
    }

    @Test
    void eventoPublicadoNoExchangeChegaAoConsumidorEGeraAlerta() {
        rabbitTemplate.convertAndSend(
            TopologiaEventos.EXCHANGE,
            TopologiaEventos.RK_CHECKLIST_ALTERADO,
            eventoChecklist("PENDENTE"));

        aguardar("o alerta ser criado a partir da mensagem",
            () -> !alertaRepository.findByGestanteId(GESTANTE_ID).isEmpty());

        var alertas = alertaRepository.findByGestanteId(GESTANTE_ID);
        assertEquals(1, alertas.size());
        assertEquals(10L, alertas.getFirst().getOrigemId());
        assertEquals("Maria Silva", alertas.getFirst().getGestanteNome());
    }

    @Test
    void eventoDeRemocaoChegaPelaPropriaFilaEApagaOsAlertas() {
        rabbitTemplate.convertAndSend(
            TopologiaEventos.EXCHANGE,
            TopologiaEventos.RK_CHECKLIST_ALTERADO,
            eventoChecklist("PENDENTE"));
        aguardar("o alerta ser criado",
            () -> !alertaRepository.findByGestanteId(GESTANTE_ID).isEmpty());

        rabbitTemplate.convertAndSend(
            TopologiaEventos.EXCHANGE,
            TopologiaEventos.RK_GESTANTE_REMOVIDA,
            Map.of(
                "eventoId", UUID.randomUUID().toString(),
                "tipo", "GESTANTE_REMOVIDA",
                "versao", 1,
                "ocorridoEm", Instant.now().toString(),
                "origem", "gestarafeto-principal",
                "gestanteId", GESTANTE_ID));

        aguardar("os alertas serem removidos",
            () -> alertaRepository.findByGestanteId(GESTANTE_ID).isEmpty());
    }

    @Test
    void routingKeyDesconhecidaNaoChegaAsFilasDoServico() {
        // Um evento de outro dominio passa pelo mesmo exchange sem perturbar este consumidor:
        // e o binding por routing key que decide quem recebe o que.
        rabbitTemplate.convertAndSend(
            TopologiaEventos.EXCHANGE, "consulta.registrada",
            Map.of("eventoId", UUID.randomUUID().toString(), "gestanteId", GESTANTE_ID));

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(alertaRepository.findByGestanteId(GESTANTE_ID).isEmpty(),
            "mensagem com routing key nao vinculada nao deveria ser processada");
    }

    @Test
    void mensagemInvalidaTerminaNaFilaDeDeadLetter() {
        rabbitTemplate.convertAndSend(
            TopologiaEventos.EXCHANGE,
            TopologiaEventos.RK_CHECKLIST_ALTERADO,
            Map.of(
                "eventoId", UUID.randomUUID().toString(),
                "tipo", "CHECKLIST_ALTERADO",
                "versao", 1,
                "ocorridoEm", Instant.now().toString(),
                "origem", "gestarafeto-principal",
                "gestanteNome", "Sem id",
                "itens", List.of()));

        RabbitAdmin admin = (RabbitAdmin) amqpAdmin;
        aguardar("a mensagem invalida cair na DLQ", () -> {
            QueueInformation dlq =
                admin.getQueueInfo(TopologiaEventos.FILA_CHECKLIST_ALTERADO_DLQ);
            return dlq != null && dlq.getMessageCount() > 0;
        });

        assertTrue(alertaRepository.findByGestanteId(GESTANTE_ID).isEmpty());
    }
}
