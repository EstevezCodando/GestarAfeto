package estevezalvarez.gestarafeto.alertas.repository;

import estevezalvarez.gestarafeto.alertas.domain.Alerta;
import estevezalvarez.gestarafeto.alertas.domain.PrioridadeAlerta;
import estevezalvarez.gestarafeto.alertas.domain.StatusAlerta;
import estevezalvarez.gestarafeto.alertas.domain.TipoAlerta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class AlertaRepositoryTest {

    @Autowired
    private AlertaRepository repository;

    private Alerta alerta(Long gestanteId, Long origemId, TipoAlerta tipo,
            PrioridadeAlerta prioridade, StatusAlerta status) {
        return Alerta.builder()
            .gestanteId(gestanteId)
            .gestanteNome("Maria")
            .origemId(origemId)
            .tipo(tipo)
            .prioridade(prioridade)
            .status(status)
            .titulo("Titulo")
            .mensagem("Mensagem")
            .dataReferencia(LocalDate.now())
            .build();
    }

    @Test
    void impedeAlertaDuplicadoParaMesmaOrigemETipo() {
        repository.saveAndFlush(alerta(1L, 10L, TipoAlerta.PROCEDIMENTO_ATRASADO,
            PrioridadeAlerta.ALTA, StatusAlerta.ABERTO));

        assertThrows(RuntimeException.class, () -> repository.saveAndFlush(
            alerta(1L, 10L, TipoAlerta.PROCEDIMENTO_ATRASADO,
                PrioridadeAlerta.ALTA, StatusAlerta.ABERTO)));
    }

    @Test
    void permiteTiposDiferentesParaMesmaOrigem() {
        repository.saveAndFlush(alerta(2L, 20L, TipoAlerta.PROCEDIMENTO_ATRASADO,
            PrioridadeAlerta.ALTA, StatusAlerta.ABERTO));
        repository.saveAndFlush(alerta(2L, 20L, TipoAlerta.REVISAO_SOLICITADA,
            PrioridadeAlerta.ALTA, StatusAlerta.ABERTO));

        assertEquals(2, repository.findByGestanteId(2L).size());
    }

    @Test
    void listaAtivosOrdenadosDaMaiorParaAMenorPrioridade() {
        repository.saveAndFlush(alerta(3L, 30L, TipoAlerta.JANELA_PROXIMA,
            PrioridadeAlerta.BAIXA, StatusAlerta.ABERTO));
        repository.saveAndFlush(alerta(3L, 31L, TipoAlerta.PROCEDIMENTO_ATRASADO,
            PrioridadeAlerta.ALTA, StatusAlerta.ABERTO));
        repository.saveAndFlush(alerta(3L, 32L, TipoAlerta.PROCEDIMENTO_PENDENTE,
            PrioridadeAlerta.MEDIA, StatusAlerta.LIDO));

        List<Alerta> ativos = repository
            .findAtivosOrdenadosPorSeveridade(
                3L, List.of(StatusAlerta.ABERTO, StatusAlerta.LIDO));

        assertEquals(3, ativos.size());
        assertEquals(PrioridadeAlerta.ALTA, ativos.get(0).getPrioridade());
        assertEquals(PrioridadeAlerta.MEDIA, ativos.get(1).getPrioridade());
        assertEquals(PrioridadeAlerta.BAIXA, ativos.get(2).getPrioridade());
    }

    @Test
    void alertasResolvidosFicamForaDaListaDeAtivos() {
        repository.saveAndFlush(alerta(4L, 40L, TipoAlerta.PROCEDIMENTO_ATRASADO,
            PrioridadeAlerta.ALTA, StatusAlerta.RESOLVIDO));

        assertTrue(repository
            .findAtivosOrdenadosPorSeveridade(
                4L, List.of(StatusAlerta.ABERTO, StatusAlerta.LIDO))
            .isEmpty());
    }

    @Test
    void contaAtivosAgrupadosPorPrioridade() {
        repository.saveAndFlush(alerta(5L, 50L, TipoAlerta.PROCEDIMENTO_ATRASADO,
            PrioridadeAlerta.ALTA, StatusAlerta.ABERTO));
        repository.saveAndFlush(alerta(5L, 51L, TipoAlerta.REVISAO_SOLICITADA,
            PrioridadeAlerta.ALTA, StatusAlerta.LIDO));
        repository.saveAndFlush(alerta(5L, 52L, TipoAlerta.JANELA_PROXIMA,
            PrioridadeAlerta.BAIXA, StatusAlerta.ABERTO));
        repository.saveAndFlush(alerta(5L, 53L, TipoAlerta.PROCEDIMENTO_PENDENTE,
            PrioridadeAlerta.MEDIA, StatusAlerta.RESOLVIDO));

        var contagens = repository.contarAtivosPorPrioridade(
            5L, List.of(StatusAlerta.ABERTO, StatusAlerta.LIDO));

        long alta = 0;
        long baixa = 0;
        for (Object[] linha : contagens) {
            if (linha[0] == PrioridadeAlerta.ALTA) {
                alta = (Long) linha[1];
            }
            if (linha[0] == PrioridadeAlerta.BAIXA) {
                baixa = (Long) linha[1];
            }
        }

        assertEquals(2, alta);
        assertEquals(1, baixa);
        assertEquals(2, contagens.size());
    }

    @Test
    void filtraPorStatusDeFormaPaginada() {
        repository.saveAndFlush(alerta(6L, 60L, TipoAlerta.PROCEDIMENTO_ATRASADO,
            PrioridadeAlerta.ALTA, StatusAlerta.ABERTO));
        repository.saveAndFlush(alerta(6L, 61L, TipoAlerta.JANELA_PROXIMA,
            PrioridadeAlerta.BAIXA, StatusAlerta.RESOLVIDO));

        var pagina = repository.findByGestanteIdAndStatus(
            6L, StatusAlerta.ABERTO, PageRequest.of(0, 10));

        assertEquals(1, pagina.getTotalElements());
        assertEquals(60L, pagina.getContent().getFirst().getOrigemId());
    }

    @Test
    void removeTodosOsAlertasDeUmaGestante() {
        repository.saveAndFlush(alerta(7L, 70L, TipoAlerta.PROCEDIMENTO_ATRASADO,
            PrioridadeAlerta.ALTA, StatusAlerta.ABERTO));
        repository.saveAndFlush(alerta(7L, 71L, TipoAlerta.JANELA_PROXIMA,
            PrioridadeAlerta.BAIXA, StatusAlerta.ABERTO));

        assertEquals(2, repository.deleteByGestanteId(7L));
        assertTrue(repository.findByGestanteId(7L).isEmpty());
    }
}
