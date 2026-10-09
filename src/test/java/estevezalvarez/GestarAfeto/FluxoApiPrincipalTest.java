package estevezalvarez.GestarAfeto;

import com.jayway.jsonpath.JsonPath;
import estevezalvarez.GestarAfeto.alerta.client.AlertaClient;
import estevezalvarez.GestarAfeto.mensageria.EventoPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de regressao das funcionalidades essenciais pela API HTTP: o caminho que a usuaria
 * percorre (cadastrar gestante, gerar checklist, registrar progresso e consultas), do JSON de
 * entrada ao banco, com o broker e o microsservico de alertas substituidos por mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/db/envers-test-schema.sql")
@TestPropertySource(properties = "gestarafeto.alertas.integracao-automatica=true")
class FluxoApiPrincipalTest {

    @MockitoBean
    private AlertaClient alertaClient;

    @MockitoBean
    private EventoPublisher eventoPublisher;

    @Autowired
    private MockMvc mockMvc;

    private long id(ResultActions resultado) throws Exception {
        String corpo = resultado.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private long criarProcedimento(String nome) throws Exception {
        return id(mockMvc.perform(post("/api/procedimentos").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"%s","descricao":"d","tipo":"EXAME","semanaInicialRecomendada":1,
                     "semanaFinalRecomendada":40,"obrigatorio":true}""".formatted(nome)))
            .andExpect(status().isCreated()));
    }

    private long criarGestante(String nome, String email) throws Exception {
        String dpp = LocalDate.now().plusWeeks(14).toString();
        return id(mockMvc.perform(post("/api/gestantes").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"%s","email":"%s","dataProvavelParto":"%s"}""".formatted(nome, email, dpp)))
            .andExpect(status().isCreated()));
    }

    @Test
    void cicloDeVidaDaGestante() throws Exception {
        long id = criarGestante("Camila Torres", "camila@example.com");

        mockMvc.perform(get("/api/gestantes/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Camila Torres"));

        mockMvc.perform(get("/api/gestantes/paginado").param("nome", "camila"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].email").value("camila@example.com"));

        mockMvc.perform(put("/api/gestantes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Camila T. Souza","email":"camila@example.com","dataProvavelParto":"%s"}"""
                    .formatted(LocalDate.now().plusWeeks(14))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Camila T. Souza"));

        mockMvc.perform(delete("/api/gestantes/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/gestantes/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void checklistGeradoEAtualizadoPublicaEventosParaOMicrosservico() throws Exception {
        criarProcedimento("Hemograma de teste do fluxo");
        long gestante = criarGestante("Dora Nunes", "dora@example.com");

        ResultActions geracao = mockMvc.perform(post("/api/gestantes/" + gestante + "/checklist/gerar"))
            .andExpect(status().isCreated());
        String corpo = geracao.andReturn().getResponse().getContentAsString();
        long item = ((Number) JsonPath.read(corpo, "$[0].id")).longValue();

        mockMvc.perform(get("/api/gestantes/" + gestante + "/checklist").param("status", "PENDENTE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(item));

        mockMvc.perform(patch("/api/checklist/" + item + "/realizar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REALIZADO"));

        mockMvc.perform(patch("/api/checklist/" + item + "/revisar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PRECISA_REVISAR"));

        mockMvc.perform(patch("/api/checklist/" + item + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REALIZADO\",\"dataRealizacao\":\"2026-07-01\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REALIZADO"));

        // Cada alteracao relevante deve anunciar o evento; o servico principal nao chama o alertas.
        verify(eventoPublisher, atLeastOnce()).publicarChecklistAlterado(any());
    }

    @Test
    void removerGestantePublicaEventoDeRemocao() throws Exception {
        long gestante = criarGestante("Elisa Prado", "elisa@example.com");

        mockMvc.perform(delete("/api/gestantes/" + gestante)).andExpect(status().isNoContent());

        verify(eventoPublisher, atLeastOnce()).publicarGestanteRemovida(any());
    }

    @Test
    void consultasSaoRegistradasListadasAtualizadasERemovidas() throws Exception {
        long gestante = criarGestante("Fabiana Reis", "fabiana@example.com");

        long consulta = id(mockMvc.perform(post("/api/gestantes/" + gestante + "/consultas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dataConsulta\":\"2026-07-17\",\"semanaGestacional\":20,\"peso\":68.50,\"pressaoArterial\":\"110/70\"}"))
            .andExpect(status().isCreated()));

        mockMvc.perform(get("/api/gestantes/" + gestante + "/consultas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].pressaoArterial").value("110/70"));
        mockMvc.perform(get("/api/gestantes/" + gestante + "/consultas/paginado"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/consultas/" + consulta)).andExpect(status().isOk());

        mockMvc.perform(put("/api/consultas/" + consulta).contentType(MediaType.APPLICATION_JSON)
                .content("{\"dataConsulta\":\"2026-07-17\",\"semanaGestacional\":21,\"peso\":69.00,\"pressaoArterial\":\"120/80\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.semanaGestacional").value(21));

        mockMvc.perform(delete("/api/consultas/" + consulta)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/consultas/" + consulta)).andExpect(status().isNotFound());
    }

    @Test
    void procedimentosPodemSerCadastradosAtualizadosFiltradosERemovidos() throws Exception {
        long proc = criarProcedimento("Exame exclusivo do teste de procedimento");

        mockMvc.perform(get("/api/procedimentos/" + proc))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("EXAME"));
        mockMvc.perform(get("/api/procedimentos/paginado").param("nome", "exclusivo do teste"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/procedimentos/paginado").param("tipo", "EXAME"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].tipo").value("EXAME"));
        mockMvc.perform(get("/api/procedimentos")).andExpect(status().isOk());

        mockMvc.perform(put("/api/procedimentos/" + proc).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Exame renomeado\",\"tipo\":\"CONSULTA\",\"obrigatorio\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Exame renomeado"));

        mockMvc.perform(delete("/api/procedimentos/" + proc)).andExpect(status().isNoContent());
    }

    @Test
    void auditoriaRegistraAsRevisoesDaGestante() throws Exception {
        long gestante = criarGestante("Gabriela Melo", "gabriela@example.com");

        mockMvc.perform(get("/api/auditoria/gestante/" + gestante))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void entradasInvalidasSaoRejeitadasComMensagemClara() throws Exception {
        mockMvc.perform(post("/api/procedimentos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"tipo\":\"EXAME\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        long gestante = criarGestante("Helena Dias", "helena@example.com");
        mockMvc.perform(post("/api/gestantes/" + gestante + "/consultas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"semanaGestacional\":10}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/gestantes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Sem datas\"}"))
            .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/checklist/987654/realizar")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/gestantes/987654/checklist/gerar")).andExpect(status().isNotFound());
    }
}
