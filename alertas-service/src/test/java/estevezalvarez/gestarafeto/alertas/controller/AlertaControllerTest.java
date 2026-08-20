package estevezalvarez.gestarafeto.alertas.controller;

import tools.jackson.databind.ObjectMapper;
import estevezalvarez.gestarafeto.alertas.dto.AvaliarChecklistRequest;
import estevezalvarez.gestarafeto.alertas.dto.ItemChecklistSnapshot;
import estevezalvarez.gestarafeto.alertas.repository.AlertaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlertaControllerTest {

    private static final Long GESTANTE_ID = 800L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlertaRepository alertaRepository;

    @BeforeEach
    void limpar() {
        alertaRepository.deleteAll();
    }

    private AvaliarChecklistRequest requestValido() {
        return new AvaliarChecklistRequest(
            GESTANTE_ID,
            "Maria Silva",
            LocalDate.now().minusWeeks(29),
            null,
            List.of(new ItemChecklistSnapshot(1L, "Hemograma", "EXAME", "PENDENTE", true, 1, 13, null)));
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Test
    void avaliarRetornaAlertasCalculados() throws Exception {
        mockMvc.perform(post("/api/alertas/avaliacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(requestValido())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].tipo").value("PROCEDIMENTO_ATRASADO"))
            .andExpect(jsonPath("$[0].prioridade").value("ALTA"))
            .andExpect(jsonPath("$[0].status").value("ABERTO"))
            .andExpect(jsonPath("$[0].gestanteNome").value("Maria Silva"));
    }

    @Test
    void avaliarRejeitaRequisicaoSemGestante() throws Exception {
        String body = """
            {"gestanteNome":"Maria","itens":[]}
            """;

        mockMvc.perform(post("/api/alertas/avaliacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listaAlertasAtivosDaGestante() throws Exception {
        mockMvc.perform(post("/api/alertas/avaliacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(requestValido())));

        mockMvc.perform(get("/api/alertas/gestante/{id}", GESTANTE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void retornaResumoAgregado() throws Exception {
        mockMvc.perform(post("/api/alertas/avaliacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(requestValido())));

        mockMvc.perform(get("/api/alertas/gestante/{id}/resumo", GESTANTE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalAtivos").value(1))
            .andExpect(jsonPath("$.alta").value(1))
            .andExpect(jsonPath("$.naoLidos").value(1));
    }

    @Test
    void listagemPaginadaAceitaFiltroDeStatus() throws Exception {
        mockMvc.perform(post("/api/alertas/avaliacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(requestValido())));

        mockMvc.perform(get("/api/alertas")
                .param("gestanteId", GESTANTE_ID.toString())
                .param("status", "ABERTO")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void marcaAlertaComoLido() throws Exception {
        String resposta = mockMvc.perform(post("/api/alertas/avaliacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(requestValido())))
            .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(resposta).get(0).get("id").asLong();

        mockMvc.perform(patch("/api/alertas/{id}/leitura", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("LIDO"))
            .andExpect(jsonPath("$.dataLeitura").exists());
    }

    @Test
    void resolverAlertaRemoveDaListaDeAtivos() throws Exception {
        String resposta = mockMvc.perform(post("/api/alertas/avaliacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(requestValido())))
            .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(resposta).get(0).get("id").asLong();

        mockMvc.perform(patch("/api/alertas/{id}/resolucao", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESOLVIDO"));

        mockMvc.perform(get("/api/alertas/gestante/{id}", GESTANTE_ID))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void buscarAlertaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/alertas/{id}", 999999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void removePorGestanteRetorna204() throws Exception {
        mockMvc.perform(post("/api/alertas/avaliacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(requestValido())));

        mockMvc.perform(delete("/api/alertas/gestante/{id}", GESTANTE_ID))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/alertas/gestante/{id}", GESTANTE_ID))
            .andExpect(jsonPath("$.length()").value(0));
    }
}
