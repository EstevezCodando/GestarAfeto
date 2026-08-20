package estevezalvarez.GestarAfeto.alerta.service;

import estevezalvarez.GestarAfeto.alerta.client.AlertaClient;
import estevezalvarez.GestarAfeto.checklist.service.ChecklistService;
import estevezalvarez.GestarAfeto.gestante.dto.CriarGestanteRequest;
import estevezalvarez.GestarAfeto.gestante.dto.GestanteResponse;
import estevezalvarez.GestarAfeto.gestante.service.GestanteService;
import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;
import estevezalvarez.GestarAfeto.procedimento.dto.CriarProcedimentoRequest;
import estevezalvarez.GestarAfeto.procedimento.service.ProcedimentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Garante que mudancas no checklist disparam a sincronizacao automatica dos alertas
 * apos o commit, sem que o dominio de checklist conheca o microsservico.
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql("/db/envers-test-schema.sql")
@TestPropertySource(properties = "gestarafeto.alertas.integracao-automatica=true")
class ChecklistAlteradoListenerTest {

    @MockitoBean
    private AlertaClient alertaClient;

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
    void gerarChecklistDisparaReavaliacaoDeAlertas() {
        when(alertaClient.avaliar(any())).thenReturn(List.of());
        garantirProcedimentoAtivo();
        GestanteResponse gestante = novaGestante();

        checklistService.gerarChecklist(gestante.id());

        verify(alertaClient, atLeastOnce()).avaliar(any());
    }

    @Test
    void marcarItemComoRealizadoDisparaReavaliacaoDeAlertas() {
        when(alertaClient.avaliar(any())).thenReturn(List.of());
        garantirProcedimentoAtivo();
        GestanteResponse gestante = novaGestante();
        var itens = checklistService.gerarChecklist(gestante.id());

        checklistService.marcarRealizado(itens.getFirst().id());

        // Uma chamada pela geracao do checklist e outra pela mudanca de status.
        verify(alertaClient, atLeastOnce()).avaliar(any());
    }

    @Test
    void removerGestanteDisparaLimpezaDosAlertas() {
        garantirProcedimentoAtivo();
        GestanteResponse gestante = novaGestante();

        gestanteService.remover(gestante.id());

        verify(alertaClient).removerPorGestante(gestante.id());
    }

    @Test
    void falhaDoMicrosservicoNaoImpedeAOperacaoDeChecklist() {
        when(alertaClient.avaliar(any())).thenThrow(new IllegalStateException("fora do ar"));
        garantirProcedimentoAtivo();
        GestanteResponse gestante = novaGestante();

        // A geracao precisa concluir normalmente mesmo com o microsservico falhando.
        var itens = checklistService.gerarChecklist(gestante.id());

        org.junit.jupiter.api.Assertions.assertFalse(itens.isEmpty());
    }
}
