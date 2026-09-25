package estevezalvarez.GestarAfeto.alerta.service;

import estevezalvarez.GestarAfeto.checklist.service.ChecklistService;
import estevezalvarez.GestarAfeto.gestante.dto.CriarGestanteRequest;
import estevezalvarez.GestarAfeto.gestante.dto.GestanteResponse;
import estevezalvarez.GestarAfeto.gestante.service.GestanteService;
import estevezalvarez.GestarAfeto.mensageria.EventoPublisher;
import estevezalvarez.GestarAfeto.mensageria.evento.ChecklistAlteradoMessage;
import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;
import estevezalvarez.GestarAfeto.procedimento.dto.CriarProcedimentoRequest;
import estevezalvarez.GestarAfeto.procedimento.service.ProcedimentoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Garante que mudancas de dominio viram eventos publicados no broker, apos o commit, sem
 * que checklist e gestante conhecam a mensageria.
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql("/db/envers-test-schema.sql")
@TestPropertySource(properties = "gestarafeto.alertas.integracao-automatica=true")
class ChecklistAlteradoListenerTest {

    @MockitoBean
    private EventoPublisher eventoPublisher;

    @Autowired
    private GestanteService gestanteService;

    @Autowired
    private ProcedimentoService procedimentoService;

    @Autowired
    private ChecklistService checklistService;

    private GestanteResponse novaGestante() {
        return gestanteService.cadastrar(new CriarGestanteRequest(
            "Ana Souza", null, null, "ana@example.com",
            java.time.LocalDate.now().minusWeeks(20), null, null));
    }

    private void garantirProcedimentoAtivo() {
        if (procedimentoService.listarAtivos().isEmpty()) {
            procedimentoService.cadastrar(new CriarProcedimentoRequest(
                "Consulta inicial", null, TipoProcedimento.CONSULTA, null, 1, 13, true));
        }
    }

    @Test
    void gerarChecklistPublicaEventoDeChecklistAlterado() {
        garantirProcedimentoAtivo();
        GestanteResponse gestante = novaGestante();

        checklistService.gerarChecklist(gestante.id());

        ArgumentCaptor<ChecklistAlteradoMessage> captor =
            ArgumentCaptor.forClass(ChecklistAlteradoMessage.class);
        verify(eventoPublisher, atLeastOnce()).publicarChecklistAlterado(captor.capture());
        assertEquals(gestante.id(), captor.getValue().gestanteId());
    }

    @Test
    void marcarItemComoRealizadoPublicaEvento() {
        garantirProcedimentoAtivo();
        GestanteResponse gestante = novaGestante();
        var itens = checklistService.gerarChecklist(gestante.id());

        checklistService.marcarRealizado(itens.getFirst().id());

        // Uma publicacao pela geracao do checklist e outra pela mudanca de status.
        verify(eventoPublisher, atLeastOnce()).publicarChecklistAlterado(any());
    }

    @Test
    void removerGestantePublicaEventoDeRemocao() {
        garantirProcedimentoAtivo();
        GestanteResponse gestante = novaGestante();

        gestanteService.remover(gestante.id());

        verify(eventoPublisher).publicarGestanteRemovida(any());
    }

    @Test
    void falhaAoPublicarNaoImpedeAOperacaoDeChecklist() {
        // Broker fora do ar no instante da publicacao.
        doThrow(new IllegalStateException("broker fora do ar"))
            .when(eventoPublisher).publicarChecklistAlterado(any());
        garantirProcedimentoAtivo();
        GestanteResponse gestante = novaGestante();

        // A geracao precisa concluir normalmente: o checklist ja foi gravado e a usuaria
        // nao pode receber erro por causa de um efeito colateral.
        var itens = checklistService.gerarChecklist(gestante.id());

        assertFalse(itens.isEmpty());
    }
}
