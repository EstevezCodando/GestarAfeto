package estevezalvarez.GestarAfeto.alerta.service;

import estevezalvarez.GestarAfeto.checklist.service.ChecklistService;
import estevezalvarez.GestarAfeto.gestante.dto.CriarGestanteRequest;
import estevezalvarez.GestarAfeto.gestante.dto.GestanteResponse;
import estevezalvarez.GestarAfeto.gestante.service.GestanteService;
import estevezalvarez.GestarAfeto.mensageria.EventoPublisher;
import estevezalvarez.GestarAfeto.mensageria.evento.ChecklistAlteradoMessage;
import estevezalvarez.GestarAfeto.mensageria.evento.GestanteRemovidaMessage;
import estevezalvarez.GestarAfeto.mensageria.evento.ItemChecklistEvento;
import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;
import estevezalvarez.GestarAfeto.procedimento.dto.CriarProcedimentoRequest;
import estevezalvarez.GestarAfeto.procedimento.service.ProcedimentoService;
import estevezalvarez.GestarAfeto.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Verifica a montagem da mensagem publicada no RabbitMQ a partir do dominio local.
 *
 * <p>O {@link EventoPublisher} e substituido por um mock: o objetivo aqui e o conteudo do
 * evento, nao o transporte. A entrega real ao broker e coberta pelo teste com Testcontainers
 * no microsservico.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql("/db/envers-test-schema.sql")
class AlertaIntegracaoServiceTest {

    @MockitoBean
    private EventoPublisher eventoPublisher;

    @Autowired
    private AlertaIntegracaoService alertaIntegracaoService;

    @Autowired
    private GestanteService gestanteService;

    @Autowired
    private ProcedimentoService procedimentoService;

    @Autowired
    private ChecklistService checklistService;

    private GestanteResponse gestanteComChecklist() {
        procedimentoService.cadastrar(new CriarProcedimentoRequest(
            "Hemograma Completo",
            "Avaliacao de celulas do sangue",
            TipoProcedimento.EXAME,
            null,
            1,
            13,
            true));

        GestanteResponse gestante = gestanteService.cadastrar(new CriarGestanteRequest(
            "Maria Silva",
            null,
            null,
            "maria@example.com",
            LocalDate.now().minusWeeks(29),
            null,
            null));

        checklistService.gerarChecklist(gestante.id());
        return gestante;
    }

    @Test
    void publicaEventoComOEstadoCompletoDoChecklist() {
        GestanteResponse gestante = gestanteComChecklist();

        alertaIntegracaoService.solicitarReavaliacao(gestante.id());

        ArgumentCaptor<ChecklistAlteradoMessage> captor =
            ArgumentCaptor.forClass(ChecklistAlteradoMessage.class);
        verify(eventoPublisher).publicarChecklistAlterado(captor.capture());

        ChecklistAlteradoMessage enviado = captor.getValue();
        assertEquals(gestante.id(), enviado.gestanteId());
        assertEquals("Maria Silva", enviado.gestanteNome());
        assertEquals(gestante.dataUltimaMenstruacao(), enviado.dataUltimaMenstruacao());
        assertTrue(enviado.itens().size() >= 1);

        // O consumidor precisa conseguir processar sem chamar de volta: as janelas
        // recomendadas do procedimento viajam junto com o item.
        ItemChecklistEvento item = enviado.itens().stream()
            .filter(i -> "Hemograma Completo".equals(i.procedimentoNome()))
            .findFirst()
            .orElseThrow();
        assertNotNull(item.itemId());
        assertEquals("PENDENTE", item.status());
        assertEquals("EXAME", item.procedimentoTipo());
        assertTrue(item.obrigatorio());
        assertEquals(1, item.semanaInicialRecomendada());
        assertEquals(13, item.semanaFinalRecomendada());
    }

    @Test
    void mensagemCarregaEnvelopeDeRastreamento() {
        GestanteResponse gestante = gestanteComChecklist();

        alertaIntegracaoService.solicitarReavaliacao(gestante.id());

        ArgumentCaptor<ChecklistAlteradoMessage> captor =
            ArgumentCaptor.forClass(ChecklistAlteradoMessage.class);
        verify(eventoPublisher).publicarChecklistAlterado(captor.capture());

        ChecklistAlteradoMessage enviado = captor.getValue();
        assertNotNull(enviado.eventoId(), "eventoId e a chave para rastrear a mensagem nos dois lados");
        assertEquals(ChecklistAlteradoMessage.TIPO, enviado.tipo());
        assertEquals(ChecklistAlteradoMessage.VERSAO_ATUAL, enviado.versao());
        assertEquals("gestarafeto-principal", enviado.origem());
        assertNotNull(enviado.ocorridoEm());
    }

    @Test
    void publicaEventoDeRemocaoComOIdentificadorDaGestante() {
        alertaIntegracaoService.solicitarRemocaoDosAlertas(77L);

        ArgumentCaptor<GestanteRemovidaMessage> captor =
            ArgumentCaptor.forClass(GestanteRemovidaMessage.class);
        verify(eventoPublisher).publicarGestanteRemovida(captor.capture());

        assertEquals(77L, captor.getValue().gestanteId());
        assertEquals(GestanteRemovidaMessage.TIPO, captor.getValue().tipo());
    }

    @Test
    void gestanteInexistenteFalhaAntesDePublicarQualquerEvento() {
        assertThrows(RecursoNaoEncontradoException.class,
            () -> alertaIntegracaoService.solicitarReavaliacao(999999L));

        verifyNoInteractions(eventoPublisher);
    }
}
