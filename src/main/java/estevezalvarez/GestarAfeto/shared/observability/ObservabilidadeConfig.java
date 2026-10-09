package estevezalvarez.GestarAfeto.shared.observability;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ajustes de observabilidade compartilhados pela API.
 *
 * <ul>
 *   <li>As sondas do Actuator (chamadas a cada poucos segundos pelo Kubernetes) nao geram
 *       trace: sem isso elas encheriam o Jaeger e esconderiam o trafego real.</li>
 *   <li>Toda resposta HTTP devolve o {@code X-Trace-Id}, o que permite ao usuario (ou ao
 *       front-end) citar o identificador ao reportar um erro e achar o trace e os logs
 *       correspondentes sem adivinhar.</li>
 * </ul>
 */
@Configuration
public class ObservabilidadeConfig {

    public static final String CABECALHO_TRACE_ID = "X-Trace-Id";

    @Bean
    ObservationPredicate ignorarSondasDoActuator() {
        return (nome, contexto) -> !(contexto instanceof ServerRequestObservationContext http
            && http.getCarrier() != null
            && http.getCarrier().getRequestURI().startsWith("/actuator"));
    }

    /**
     * O Spring Boot cria o exportador OTLP de logs, mas nao liga o Logback a ele. Esta ponte
     * entrega ao appender {@code OTEL} (logback-spring.xml) o SDK ja configurado; so existe
     * quando a exportacao esta habilitada, entao testes e execucao local nao sao afetados.
     */
    @Bean
    @ConditionalOnProperty(name = "management.logging.export.otlp.enabled", havingValue = "true")
    InitializingBean ligarLogbackAoOpenTelemetry(OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }

    @Bean
    TraceIdResponseFilter traceIdResponseFilter(Tracer tracer) {
        return new TraceIdResponseFilter(tracer);
    }

    /** Roda depois do filtro de observacao do Spring (que abre o span) e antes dos controllers. */
    static class TraceIdResponseFilter extends OncePerRequestFilter implements Ordered {

        private final Tracer tracer;

        TraceIdResponseFilter(Tracer tracer) {
            this.tracer = tracer;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            Span span = tracer.currentSpan();
            if (span != null) {
                response.setHeader(CABECALHO_TRACE_ID, span.context().traceId());
            }
            chain.doFilter(request, response);
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE + 10;
        }
    }
}
