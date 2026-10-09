package estevezalvarez.GestarAfeto.shared.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Garante o comportamento de rastreamento sem depender de coletor, banco ou broker.
 */
class ObservabilidadeConfigTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    private final ObservabilidadeConfig config = new ObservabilidadeConfig();

    private ServerRequestObservationContext contextoHttp(String uri) {
        return new ServerRequestObservationContext(
            new MockHttpServletRequest("GET", uri), new MockHttpServletResponse());
    }

    @Test
    void sondasDoActuatorNaoGeramTrace() {
        ObservationPredicate predicado = config.ignorarSondasDoActuator();

        assertFalse(predicado.test("http.server.requests", contextoHttp("/actuator/health/liveness")));
        assertFalse(predicado.test("http.server.requests", contextoHttp("/actuator/health/readiness")));
    }

    @Test
    void requisicoesDeNegocioContinuamRastreadas() {
        ObservationPredicate predicado = config.ignorarSondasDoActuator();

        assertTrue(predicado.test("http.server.requests", contextoHttp("/api/gestantes")));
    }

    @Test
    void observacoesQueNaoSaoHttpNaoSaoAfetadas() {
        ObservationPredicate predicado = config.ignorarSondasDoActuator();

        assertTrue(predicado.test("spring.rabbit.template", new Observation.Context()));
    }

    @Test
    void respostaCarregaOTraceIdDoSpanAtual() throws Exception {
        Tracer tracer = tracerComSpan(TRACE_ID);
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        FilterChain cadeia = mock(FilterChain.class);
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/api/gestantes");

        new ObservabilidadeConfig.TraceIdResponseFilter(tracer).doFilter(requisicao, resposta, cadeia);

        assertEquals(TRACE_ID, resposta.getHeader(ObservabilidadeConfig.CABECALHO_TRACE_ID));
        verify(cadeia).doFilter(requisicao, resposta);
    }

    @Test
    void semSpanAtualARespostaSegueSemCabecalho() throws Exception {
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        FilterChain cadeia = mock(FilterChain.class);
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/actuator/health");

        new ObservabilidadeConfig.TraceIdResponseFilter(tracer).doFilter(requisicao, resposta, cadeia);

        assertNull(resposta.getHeader(ObservabilidadeConfig.CABECALHO_TRACE_ID));
        verify(cadeia).doFilter(requisicao, resposta);
    }

    private Tracer tracerComSpan(String traceId) {
        TraceContext contexto = mock(TraceContext.class);
        when(contexto.traceId()).thenReturn(traceId);
        Span span = mock(Span.class);
        when(span.context()).thenReturn(contexto);
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(span);
        return tracer;
    }
}
