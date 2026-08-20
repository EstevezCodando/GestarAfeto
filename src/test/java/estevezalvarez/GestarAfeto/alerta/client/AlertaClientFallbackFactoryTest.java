package estevezalvarez.GestarAfeto.alerta.client;

import estevezalvarez.GestarAfeto.alerta.client.dto.AvaliarChecklistRequest;
import estevezalvarez.GestarAfeto.shared.exception.ServicoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Documenta a degradacao assimetrica do fallback: leituras seguem vazias, acoes explicitas
 * da usuaria falham de forma visivel.
 */
class AlertaClientFallbackFactoryTest {

    private AlertaClient fallback;

    @BeforeEach
    void setUp() {
        fallback = new AlertaClientFallbackFactory().create(new IllegalStateException("connection refused"));
    }

    @Test
    void leiturasDegradamParaRespostaVazia() {
        assertAll(
            () -> assertTrue(fallback.listarPorGestante(1L).isEmpty()),
            () -> assertTrue(fallback.avaliar(
                new AvaliarChecklistRequest(1L, "Maria", null, null, List.of())).isEmpty()));
    }

    @Test
    void resumoDegradaParaContagensZeradas() {
        var resumo = fallback.resumo(7L);

        assertEquals(7L, resumo.gestanteId());
        assertEquals(0, resumo.totalAtivos());
        assertEquals(0, resumo.alta());
    }

    @Test
    void acoesDaUsuariaFalhamComServicoIndisponivel() {
        assertAll(
            () -> assertThrows(ServicoIndisponivelException.class, () -> fallback.marcarLido(1L)),
            () -> assertThrows(ServicoIndisponivelException.class, () -> fallback.resolver(1L)));
    }

    @Test
    void remocaoDeAlertasNaoBloqueiaARemocaoDaGestante() {
        assertDoesNotThrow(() -> fallback.removerPorGestante(1L));
    }
}
