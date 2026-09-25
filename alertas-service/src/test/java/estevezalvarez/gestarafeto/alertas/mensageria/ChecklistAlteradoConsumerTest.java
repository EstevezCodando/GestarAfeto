package estevezalvarez.gestarafeto.alertas.mensageria;

import estevezalvarez.gestarafeto.alertas.domain.StatusAlerta;
import estevezalvarez.gestarafeto.alertas.mensageria.consumer.ChecklistAlteradoConsumer;
import estevezalvarez.gestarafeto.alertas.mensageria.evento.ChecklistAlteradoMessage;
import estevezalvarez.gestarafeto.alertas.mensageria.evento.ItemChecklistEvento;
import estevezalvarez.gestarafeto.alertas.repository.AlertaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comportamento do consumidor de {@code checklist.alterado} sobre a mensagem recebida.
 *
 * <p>Invoca o metodo do listener diretamente, sem broker: o que se valida aqui e a traducao
 * da mensagem, a idempotencia e a decisao de mandar para a DLQ. O trajeto pela fila e
 * coberto por {@code EventoRabbitMqIntegrationTest}.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ChecklistAlteradoConsumerTest {

    private static final Long GESTANTE_ID = 500L;

    @Autowired
    private ChecklistAlteradoConsumer consumer;

    @Autowired
    private AlertaRepository alertaRepository;

    @BeforeEach
    void limpar() {
        alertaRepository.deleteAll();
    }

    /** DUM que coloca a gestante na semana 26, com a janela 1-13 ja vencida. */
    private ChecklistAlteradoMessage mensagem(String statusItem) {
        return new ChecklistAlteradoMessage(
            UUID.randomUUID().toString(),
            "CHECKLIST_ALTERADO",
            1,
            Instant.now(),
            "gestarafeto-principal",
            GESTANTE_ID,
            "Maria Silva",
            LocalDate.now().minusWeeks(25),
            null,
            List.of(new ItemChecklistEvento(
                10L, "Hemograma Completo", "EXAME", statusItem, true, 1, 13, null)));
    }

    @Test
    void consumirEventoGeraAlertaAPartirDoEstadoRecebido() {
        consumer.consumir(mensagem("PENDENTE"));

        var alertas = alertaRepository.findByGestanteId(GESTANTE_ID);
        assertEquals(1, alertas.size());
        assertEquals(StatusAlerta.ABERTO, alertas.getFirst().getStatus());
        assertEquals(10L, alertas.getFirst().getOrigemId());
    }

    @Test
    void consumirDuasVezesNaoDuplicaAlertas() {
        ChecklistAlteradoMessage msg = mensagem("PENDENTE");

        // O RabbitMQ garante entrega ao menos uma vez: uma reentrega nao pode duplicar.
        consumer.consumir(msg);
        consumer.consumir(msg);

        assertEquals(1, alertaRepository.findByGestanteId(GESTANTE_ID).size());
    }

    @Test
    void itemConcluidoResolveOAlertaExistente() {
        consumer.consumir(mensagem("PENDENTE"));
        consumer.consumir(mensagem("REALIZADO"));

        var alertas = alertaRepository.findByGestanteId(GESTANTE_ID);
        assertEquals(1, alertas.size());
        assertEquals(StatusAlerta.RESOLVIDO, alertas.getFirst().getStatus());
    }

    @Test
    void mensagemSemGestanteVaiDiretoParaDeadLetter() {
        ChecklistAlteradoMessage invalida = new ChecklistAlteradoMessage(
            UUID.randomUUID().toString(), "CHECKLIST_ALTERADO", 1, Instant.now(),
            "gestarafeto-principal", null, "Maria", null, null, List.of());

        // Repetir nao corrige um payload invalido: rejeitar sem reenfileirar evita
        // ocupar as tentativas e atrasar as mensagens seguintes.
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.consumir(invalida));
    }

    @Test
    void mensagemSemNomeDaGestanteVaiDiretoParaDeadLetter() {
        ChecklistAlteradoMessage invalida = new ChecklistAlteradoMessage(
            UUID.randomUUID().toString(), "CHECKLIST_ALTERADO", 1, Instant.now(),
            "gestarafeto-principal", GESTANTE_ID, "  ", null, null, List.of());

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.consumir(invalida));
    }

    @Test
    void checklistVazioNaoGeraAlerta() {
        ChecklistAlteradoMessage semItens = new ChecklistAlteradoMessage(
            UUID.randomUUID().toString(), "CHECKLIST_ALTERADO", 1, Instant.now(),
            "gestarafeto-principal", GESTANTE_ID, "Maria Silva",
            LocalDate.now().minusWeeks(25), null, List.of());

        consumer.consumir(semItens);

        assertTrue(alertaRepository.findByGestanteId(GESTANTE_ID).isEmpty());
    }
}
