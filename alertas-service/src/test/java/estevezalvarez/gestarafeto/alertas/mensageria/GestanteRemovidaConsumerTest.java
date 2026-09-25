package estevezalvarez.gestarafeto.alertas.mensageria;

import estevezalvarez.gestarafeto.alertas.mensageria.consumer.ChecklistAlteradoConsumer;
import estevezalvarez.gestarafeto.alertas.mensageria.consumer.GestanteRemovidaConsumer;
import estevezalvarez.gestarafeto.alertas.mensageria.evento.ChecklistAlteradoMessage;
import estevezalvarez.gestarafeto.alertas.mensageria.evento.GestanteRemovidaMessage;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comportamento do consumidor de {@code gestante.removida}.
 *
 * <p>Esta limpeza e a contrapartida da ausencia de chave estrangeira entre os bancos: sem
 * cascade no banco, quem apaga os dados derivados e a aplicacao, reagindo ao evento.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class GestanteRemovidaConsumerTest {

    private static final Long GESTANTE_ID = 600L;

    @Autowired
    private GestanteRemovidaConsumer consumer;

    @Autowired
    private ChecklistAlteradoConsumer checklistConsumer;

    @Autowired
    private AlertaRepository alertaRepository;

    @BeforeEach
    void limpar() {
        alertaRepository.deleteAll();
    }

    private void darAlertasA(Long gestanteId) {
        checklistConsumer.consumir(new ChecklistAlteradoMessage(
            UUID.randomUUID().toString(), "CHECKLIST_ALTERADO", 1, Instant.now(),
            "gestarafeto-principal", gestanteId, "Maria Silva",
            LocalDate.now().minusWeeks(25), null,
            List.of(new ItemChecklistEvento(
                20L, "Hemograma Completo", "EXAME", "PENDENTE", true, 1, 13, null))));
    }

    private GestanteRemovidaMessage mensagem(Long gestanteId) {
        return new GestanteRemovidaMessage(
            UUID.randomUUID().toString(), "GESTANTE_REMOVIDA", 1, Instant.now(),
            "gestarafeto-principal", gestanteId);
    }

    @Test
    void consumirEventoRemoveOsAlertasDaGestante() {
        darAlertasA(GESTANTE_ID);
        assertFalse(alertaRepository.findByGestanteId(GESTANTE_ID).isEmpty());

        consumer.consumir(mensagem(GESTANTE_ID));

        assertTrue(alertaRepository.findByGestanteId(GESTANTE_ID).isEmpty());
    }

    @Test
    void consumirDuasVezesNaoFalha() {
        darAlertasA(GESTANTE_ID);

        consumer.consumir(mensagem(GESTANTE_ID));

        // Reentrega da mesma mensagem: nada a remover, e isso nao e erro.
        assertDoesNotThrow(() -> consumer.consumir(mensagem(GESTANTE_ID)));
    }

    @Test
    void mensagemSemGestanteVaiDiretoParaDeadLetter() {
        assertThrows(AmqpRejectAndDontRequeueException.class,
            () -> consumer.consumir(mensagem(null)));
    }
}
