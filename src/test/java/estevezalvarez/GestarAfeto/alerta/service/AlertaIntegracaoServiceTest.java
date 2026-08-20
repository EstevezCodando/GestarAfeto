package estevezalvarez.GestarAfeto.alerta.service;

import estevezalvarez.GestarAfeto.alerta.client.AlertaClient;
import estevezalvarez.GestarAfeto.alerta.client.dto.AvaliarChecklistRequest;
import estevezalvarez.GestarAfeto.alerta.client.dto.ItemChecklistSnapshot;
import estevezalvarez.GestarAfeto.checklist.service.ChecklistService;
import estevezalvarez.GestarAfeto.gestante.dto.CriarGestanteRequest;
import estevezalvarez.GestarAfeto.gestante.dto.GestanteResponse;
import estevezalvarez.GestarAfeto.gestante.service.GestanteService;
import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;
import estevezalvarez.GestarAfeto.procedimento.dto.CriarProcedimentoRequest;
import estevezalvarez.GestarAfeto.procedimento.service.ProcedimentoService;
import estevezalvarez.GestarAfeto.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica a traducao do dominio do servico principal para o contrato do microsservico.
 * O {@link AlertaClient} e substituido por um mock: o objetivo aqui e o snapshot enviado,
 * nao o comportamento remoto.
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql("/db/envers-test-schema.sql")
class AlertaIntegracaoServiceTest {

    @MockitoBean
    private AlertaClient alertaClient;

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
    void enviaSnapshotDoChecklistComDadosDaGestanteEDoProcedimento() {
        GestanteResponse gestante = gestanteComChecklist();
        when(alertaClient.avaliar(any())).thenReturn(List.of());

        alertaIntegracaoService.reavaliar(gestante.id());

        ArgumentCaptor<AvaliarChecklistRequest> captor =
            ArgumentCaptor.forClass(AvaliarChecklistRequest.class);
        verify(alertaClient).avaliar(captor.capture());

        AvaliarChecklistRequest enviado = captor.getValue();
        assertEquals(gestante.id(), enviado.gestanteId());
        assertEquals("Maria Silva", enviado.gestanteNome());
        assertEquals(gestante.dataUltimaMenstruacao(), enviado.dataUltimaMenstruacao());
        assertTrue(enviado.itens().size() >= 1);

        ItemChecklistSnapshot item = enviado.itens().stream()
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
    void reavaliarGestanteInexistenteFalhaAntesDeChamarOMicrosservico() {
        assertThrows(RecursoNaoEncontradoException.class,
            () -> alertaIntegracaoService.reavaliar(999999L));
    }

    @Test
    void reavaliarSemPropagarErroEngoleFalhaDoMicrosservico() {
        GestanteResponse gestante = gestanteComChecklist();
        when(alertaClient.avaliar(any())).thenThrow(new IllegalStateException("servico fora do ar"));

        // Nao deve lancar: o gatilho automatico jamais pode quebrar a operacao de checklist.
        alertaIntegracaoService.reavaliarSemPropagarErro(gestante.id());

        verify(alertaClient).avaliar(any());
    }
}
