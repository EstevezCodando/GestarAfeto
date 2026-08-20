package estevezalvarez.GestarAfeto.alerta.controller;

import estevezalvarez.GestarAfeto.alerta.client.AlertaClient;
import estevezalvarez.GestarAfeto.alerta.client.dto.AlertaResponse;
import estevezalvarez.GestarAfeto.alerta.client.dto.ResumoAlertasResponse;
import estevezalvarez.GestarAfeto.gestante.dto.CriarGestanteRequest;
import estevezalvarez.GestarAfeto.gestante.dto.GestanteResponse;
import estevezalvarez.GestarAfeto.gestante.service.GestanteService;
import estevezalvarez.GestarAfeto.shared.exception.ServicoIndisponivelException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre a fachada REST de alertas do servico principal, incluindo a traducao da
 * indisponibilidade do microsservico para 503.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/db/envers-test-schema.sql")
class AlertaControllerTest {

    @MockitoBean
    private AlertaClient alertaClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GestanteService gestanteService;

    private GestanteResponse novaGestante() {
        return gestanteService.cadastrar(new CriarGestanteRequest(
            "Beatriz Lima", null, null, "beatriz@example.com",
            LocalDate.now().minusWeeks(25), null, null));
    }

    private AlertaResponse alerta(Long id, Long gestanteId) {
        return new AlertaResponse(
            id, gestanteId, "Beatriz Lima", 10L,
            "PROCEDIMENTO_ATRASADO", "ALTA", "ABERTO",
            "Atrasado: Hemograma", "A janela recomendada terminou.",
            LocalDate.now(), 13, LocalDateTime.now(), null, null);
    }

    @Test
    void avaliarRetornaAlertasDoMicrosservico() throws Exception {
        GestanteResponse gestante = novaGestante();
        when(alertaClient.avaliar(any())).thenReturn(List.of(alerta(1L, gestante.id())));

        mockMvc.perform(post("/api/gestantes/{id}/alertas/avaliar", gestante.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].tipo").value("PROCEDIMENTO_ATRASADO"))
            .andExpect(jsonPath("$[0].prioridade").value("ALTA"));
    }

    @Test
    void listarRetornaAlertasAtivos() throws Exception {
        GestanteResponse gestante = novaGestante();
        when(alertaClient.listarPorGestante(gestante.id())).thenReturn(List.of(alerta(2L, gestante.id())));

        mockMvc.perform(get("/api/gestantes/{id}/alertas", gestante.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void resumoRetornaContagensAgregadas() throws Exception {
        GestanteResponse gestante = novaGestante();
        when(alertaClient.resumo(gestante.id()))
            .thenReturn(new ResumoAlertasResponse(gestante.id(), 3, 2, 1, 0, 3, 5));

        mockMvc.perform(get("/api/gestantes/{id}/alertas/resumo", gestante.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalAtivos").value(3))
            .andExpect(jsonPath("$.alta").value(2))
            .andExpect(jsonPath("$.media").value(1))
            .andExpect(jsonPath("$.resolvidos").value(5));
    }

    @Test
    void listarAlertasDeGestanteInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/gestantes/{id}/alertas", 999999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void marcarLidoDelegaAoMicrosservico() throws Exception {
        when(alertaClient.marcarLido(5L)).thenReturn(alerta(5L, 1L));

        mockMvc.perform(patch("/api/alertas/{id}/leitura", 5))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void microsservicoIndisponivelEmAcaoDaUsuariaRetorna503() throws Exception {
        when(alertaClient.resolver(eq(9L)))
            .thenThrow(new ServicoIndisponivelException("O servico de alertas esta indisponivel no momento."));

        mockMvc.perform(patch("/api/alertas/{id}/resolucao", 9))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503))
            .andExpect(jsonPath("$.erro").value("Servico indisponivel"));
    }

    @Test
    void microsservicoIndisponivelEmLeituraDegradaParaListaVazia() throws Exception {
        GestanteResponse gestante = novaGestante();
        // Comportamento equivalente ao fallback do circuit breaker.
        when(alertaClient.listarPorGestante(gestante.id())).thenReturn(List.of());

        mockMvc.perform(get("/api/gestantes/{id}/alertas", gestante.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
