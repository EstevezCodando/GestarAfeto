package estevezalvarez.GestarAfeto.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Falhas do <i>cliente</i> precisam virar 4xx, nao 500. Antes da correcao no
 * {@link GlobalExceptionHandler}, o handler generico de {@code Exception} capturava as excecoes
 * de protocolo do Spring MVC e respondia 500 a qualquer URL inexistente ou JSON malformado.
 */
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
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.path").value("/api/nao-existe"));
    }

    @Test
    void metodoNaoSuportadoResponde405() throws Exception {
        mockMvc.perform(patch("/api/gestantes"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void jsonMalformadoResponde400() throws Exception {
        mockMvc.perform(post("/api/gestantes").contentType(MediaType.APPLICATION_JSON).content("{nome:"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void identificadorComTipoErradoResponde400() throws Exception {
        mockMvc.perform(get("/api/gestantes/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void validacaoDeCamposObrigatoriosResponde400() throws Exception {
        mockMvc.perform(post("/api/gestantes").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem").value("Nome é obrigatório"));
    }

    @Test
    void recursoDeDominioInexistenteResponde404ComMensagem() throws Exception {
        mockMvc.perform(get("/api/gestantes/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void erroNaoMapeadoNuncaVazaDetalhesInternos() throws Exception {
        mockMvc.perform(get("/api/nao-existe"))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
            .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
