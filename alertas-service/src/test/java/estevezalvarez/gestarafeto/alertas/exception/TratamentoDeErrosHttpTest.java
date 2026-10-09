package estevezalvarez.gestarafeto.alertas.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Falhas do cliente viram 4xx; so falhas reais do servidor podem ser 500. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TratamentoDeErrosHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void caminhoInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/api/nao-existe"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void metodoNaoSuportadoResponde405() throws Exception {
        mockMvc.perform(put("/api/alertas/1/leitura"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void jsonMalformadoResponde400() throws Exception {
        mockMvc.perform(post("/api/alertas/avaliacoes").contentType(MediaType.APPLICATION_JSON).content("{gestanteId:"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void identificadorComTipoErradoResponde400() throws Exception {
        mockMvc.perform(get("/api/alertas/gestante/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void alertaInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/api/alertas/987654"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }
}
