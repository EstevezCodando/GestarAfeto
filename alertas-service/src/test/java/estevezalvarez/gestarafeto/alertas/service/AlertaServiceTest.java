package estevezalvarez.gestarafeto.alertas.service;

import estevezalvarez.gestarafeto.alertas.domain.StatusAlerta;
import estevezalvarez.gestarafeto.alertas.domain.TipoAlerta;
import estevezalvarez.gestarafeto.alertas.dto.AlertaResponse;
import estevezalvarez.gestarafeto.alertas.dto.AvaliarChecklistRequest;
import estevezalvarez.gestarafeto.alertas.dto.ItemChecklistSnapshot;
import estevezalvarez.gestarafeto.alertas.repository.AlertaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre a reconciliacao entre o estado do checklist e os alertas ja persistidos.
 */
@SpringBootTest
@ActiveProfiles("test")
class AlertaServiceTest {

    private static final Long GESTANTE_ID = 900L;

    @Autowired
    private AlertaService alertaService;

    @Autowired
    private AlertaRepository alertaRepository;

    @BeforeEach
    void limpar() {
        alertaRepository.deleteAll();
    }

    /** DUM que coloca a gestante na semana 30, com a janela 1-13 ja vencida. */
    private LocalDate dumSemana30() {
        return LocalDate.now().minusWeeks(29);
    }

    private AvaliarChecklistRequest request(ItemChecklistSnapshot... itens) {
        return new AvaliarChecklistRequest(GESTANTE_ID, "Maria Silva", dumSemana30(), null, List.of(itens));
    }

    private ItemChecklistSnapshot item(long id, String status) {
        return new ItemChecklistSnapshot(id, "Hemograma", "EXAME", status, true, 1, 13, null);
    }

    @Test
    void primeiraAvaliacaoCriaAlertasAbertos() {
        List<AlertaResponse> alertas = alertaService.avaliar(request(item(1L, "PENDENTE"), item(2L, "PENDENTE")));

        assertEquals(2, alertas.size());
        assertTrue(alertas.stream().allMatch(a -> a.status() == StatusAlerta.ABERTO));
        assertTrue(alertas.stream().allMatch(a -> a.tipo() == TipoAlerta.PROCEDIMENTO_ATRASADO));
    }

    @Test
    void reavaliarComOMesmoSnapshotNaoDuplicaAlertas() {
        alertaService.avaliar(request(item(1L, "PENDENTE"), item(2L, "PENDENTE")));
        List<AlertaResponse> segunda = alertaService.avaliar(request(item(1L, "PENDENTE"), item(2L, "PENDENTE")));

        assertEquals(2, segunda.size());
        assertEquals(2, alertaRepository.findByGestanteId(GESTANTE_ID).size());
    }

    @Test
    void concluirItemResolveOAlertaCorrespondente() {
        alertaService.avaliar(request(item(1L, "PENDENTE"), item(2L, "PENDENTE")));

        List<AlertaResponse> ativos = alertaService.avaliar(request(item(1L, "REALIZADO"), item(2L, "PENDENTE")));

        assertEquals(1, ativos.size());
        assertEquals(2L, ativos.getFirst().origemId());

        var todos = alertaRepository.findByGestanteId(GESTANTE_ID);
        assertEquals(2, todos.size());
        var resolvido = todos.stream().filter(a -> a.getOrigemId() == 1L).findFirst().orElseThrow();
        assertEquals(StatusAlerta.RESOLVIDO, resolvido.getStatus());
        assertNotNull(resolvido.getDataResolucao());
    }

    @Test
    void itemQueVoltaAFicarPendenteReabreOAlertaJaExistente() {
        alertaService.avaliar(request(item(1L, "PENDENTE")));
        alertaService.avaliar(request(item(1L, "REALIZADO")));

        List<AlertaResponse> reabertos = alertaService.avaliar(request(item(1L, "PENDENTE")));

        assertEquals(1, reabertos.size());
        assertEquals(StatusAlerta.ABERTO, reabertos.getFirst().status());
        assertNull(reabertos.getFirst().dataResolucao());
        // Continua sendo o mesmo registro: a chave (origem, tipo) evita duplicidade.
        assertEquals(1, alertaRepository.findByGestanteId(GESTANTE_ID).size());
    }

    @Test
    void reavaliacaoPreservaOStatusDeLeitura() {
        List<AlertaResponse> criados = alertaService.avaliar(request(item(1L, "PENDENTE")));
        alertaService.marcarLido(criados.getFirst().id());

        List<AlertaResponse> reavaliados = alertaService.avaliar(request(item(1L, "PENDENTE")));

        assertEquals(StatusAlerta.LIDO, reavaliados.getFirst().status());
        assertNotNull(reavaliados.getFirst().dataLeitura());
    }

    @Test
    void mudancaDeTipoDoAlertaResolveOAnteriorECriaONovo() {
        // Pendente e vencido -> PROCEDIMENTO_ATRASADO
        alertaService.avaliar(request(item(1L, "PENDENTE")));
        // Mesmo item marcado para revisao -> REVISAO_SOLICITADA
        List<AlertaResponse> ativos = alertaService.avaliar(request(item(1L, "PRECISA_REVISAR")));

        assertEquals(1, ativos.size());
        assertEquals(TipoAlerta.REVISAO_SOLICITADA, ativos.getFirst().tipo());
        assertEquals(2, alertaRepository.findByGestanteId(GESTANTE_ID).size());
    }

    @Test
    void resumoAgregaAlertasAtivosPorPrioridade() {
        alertaService.avaliar(request(item(1L, "PENDENTE"), item(2L, "PRECISA_REVISAR")));

        var resumo = alertaService.resumo(GESTANTE_ID);

        assertEquals(2, resumo.totalAtivos());
        assertEquals(2, resumo.alta());
        assertEquals(0, resumo.media());
        assertEquals(2, resumo.naoLidos());
    }

    @Test
    void marcarLidoRegistraDataEMantemAlertaAtivo() {
        var criado = alertaService.avaliar(request(item(1L, "PENDENTE"))).getFirst();

        var lido = alertaService.marcarLido(criado.id());

        assertEquals(StatusAlerta.LIDO, lido.status());
        assertNotNull(lido.dataLeitura());
        assertEquals(1, alertaService.listarAtivos(GESTANTE_ID).size());
    }

    @Test
    void resolverManualmenteRemoveDaListaDeAtivos() {
        var criado = alertaService.avaliar(request(item(1L, "PENDENTE"))).getFirst();

        alertaService.resolver(criado.id());

        assertTrue(alertaService.listarAtivos(GESTANTE_ID).isEmpty());
    }

    @Test
    void removerPorGestanteApagaTodosOsAlertas() {
        alertaService.avaliar(request(item(1L, "PENDENTE"), item(2L, "PENDENTE")));

        assertEquals(2, alertaService.removerPorGestante(GESTANTE_ID));
        assertTrue(alertaRepository.findByGestanteId(GESTANTE_ID).isEmpty());
    }
}
