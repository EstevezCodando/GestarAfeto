package estevezalvarez.GestarAfeto.alerta.client;

import com.sun.net.httpserver.HttpServer;
import estevezalvarez.GestarAfeto.alerta.client.dto.AlertaResponse;
import estevezalvarez.GestarAfeto.alerta.client.dto.AvaliarChecklistRequest;
import estevezalvarez.GestarAfeto.alerta.client.dto.ResumoAlertasResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercita o {@link AlertaClient} sobre HTTP real, contra um servidor stub em memoria.
 *
 * <p>Os demais testes de integracao substituem o cliente por um mock, o que valida a
 * traducao do dominio mas nunca a camada de transporte. Este teste cobre justamente essa
 * lacuna — foi escrito depois que o cliente HTTP padrao do Feign
 * ({@code HttpURLConnection}) se mostrou incapaz de emitir requisicoes <b>PATCH</b>,
 * falhando com "Invalid HTTP method: PATCH" somente com os dois servicos no ar.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class AlertaClientHttpTest {

    private static HttpServer servidor;
    private static final List<String> METODOS_RECEBIDOS = new CopyOnWriteArrayList<>();

    @Autowired
    private AlertaClient alertaClient;

    @BeforeAll
    static void iniciarStub() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext("/api/alertas", troca -> {
            METODOS_RECEBIDOS.add(troca.getRequestMethod() + " " + troca.getRequestURI().getPath());
            String corpo = corpoPara(troca.getRequestURI().getPath(), troca.getRequestMethod());
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(bytes.length == 0 ? 204 : 200, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = troca.getResponseBody()) {
                    out.write(bytes);
                }
            }
            troca.close();
        });

        servidor.start();
    }

    private static String corpoPara(String caminho, String metodo) {
        if ("DELETE".equals(metodo)) {
            return "";
        }
        if (caminho.endsWith("/resumo")) {
            return """
                {"gestanteId":1,"totalAtivos":3,"alta":2,"media":1,"baixa":0,"naoLidos":3,"resolvidos":4}
                """;
        }
        if (caminho.endsWith("/leitura") || caminho.endsWith("/resolucao")) {
            return alertaJson();
        }
        return "[" + alertaJson() + "]";
    }

    private static String alertaJson() {
        return """
            {"id":7,"gestanteId":1,"gestanteNome":"Maria Silva","origemId":6,
             "tipo":"PROCEDIMENTO_ATRASADO","prioridade":"ALTA","status":"LIDO",
             "titulo":"Atrasado: Hemograma","mensagem":"A janela recomendada terminou.",
             "dataReferencia":"2026-06-10","semanaGestacionalReferencia":13,
             "dataCriacao":"2026-08-19T21:15:03","dataLeitura":"2026-08-19T21:20:00",
             "dataResolucao":null}
            """;
    }

    @AfterAll
    static void pararStub() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @DynamicPropertySource
    static void apontarClienteParaOStub(DynamicPropertyRegistry registry) {
        registry.add("gestarafeto.alertas.url",
            () -> "http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @Test
    void marcarLidoTrafegaComoPatchDeVerdade() {
        AlertaResponse resposta = alertaClient.marcarLido(7L);

        assertNotNull(resposta);
        assertEquals(7L, resposta.id());
        assertEquals("LIDO", resposta.status());
        assertTrue(METODOS_RECEBIDOS.contains("PATCH /api/alertas/7/leitura"),
            "o cliente HTTP precisa suportar PATCH; recebidos: " + METODOS_RECEBIDOS);
    }

    @Test
    void resolverTrafegaComoPatchDeVerdade() {
        alertaClient.resolver(8L);

        assertTrue(METODOS_RECEBIDOS.contains("PATCH /api/alertas/8/resolucao"),
            "o cliente HTTP precisa suportar PATCH; recebidos: " + METODOS_RECEBIDOS);
    }

    @Test
    void avaliarTrafegaComoPostComCorpoJson() {
        List<AlertaResponse> alertas = alertaClient.avaliar(
            new AvaliarChecklistRequest(1L, "Maria Silva", null, null, List.of()));

        assertEquals(1, alertas.size());
        assertEquals("PROCEDIMENTO_ATRASADO", alertas.getFirst().tipo());
        assertTrue(METODOS_RECEBIDOS.contains("POST /api/alertas/avaliacoes"));
    }

    @Test
    void listarEResumoTrafegamComoGet() {
        List<AlertaResponse> alertas = alertaClient.listarPorGestante(1L);
        ResumoAlertasResponse resumo = alertaClient.resumo(1L);

        assertEquals(1, alertas.size());
        assertEquals(3, resumo.totalAtivos());
        assertEquals(2, resumo.alta());
        assertTrue(METODOS_RECEBIDOS.contains("GET /api/alertas/gestante/1"));
        assertTrue(METODOS_RECEBIDOS.contains("GET /api/alertas/gestante/1/resumo"));
    }

    @Test
    void removerPorGestanteTrafegaComoDelete() {
        alertaClient.removerPorGestante(1L);

        assertTrue(METODOS_RECEBIDOS.contains("DELETE /api/alertas/gestante/1"));
    }
}
