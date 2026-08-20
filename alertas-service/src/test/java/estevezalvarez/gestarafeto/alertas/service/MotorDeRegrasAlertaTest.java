package estevezalvarez.gestarafeto.alertas.service;

import estevezalvarez.gestarafeto.alertas.config.AlertaRegrasProperties;
import estevezalvarez.gestarafeto.alertas.domain.PrioridadeAlerta;
import estevezalvarez.gestarafeto.alertas.domain.TipoAlerta;
import estevezalvarez.gestarafeto.alertas.dto.AvaliarChecklistRequest;
import estevezalvarez.gestarafeto.alertas.dto.ItemChecklistSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da regra de negocio propria do microsservico, sem Spring e sem banco.
 */
class MotorDeRegrasAlertaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 8, 19);

    private AlertaRegrasProperties regras;
    private MotorDeRegrasAlerta motor;

    @BeforeEach
    void setUp() {
        regras = new AlertaRegrasProperties();
        motor = new MotorDeRegrasAlerta(regras);
    }

    private ItemChecklistSnapshot item(long id, String nome, String status, boolean obrigatorio,
            Integer inicio, Integer fim) {
        return new ItemChecklistSnapshot(id, nome, "EXAME", status, obrigatorio, inicio, fim, null);
    }

    private AvaliarChecklistRequest requestComDum(LocalDate dum, ItemChecklistSnapshot... itens) {
        return new AvaliarChecklistRequest(1L, "Maria", dum, null, List.of(itens));
    }

    @Test
    void calculaSemanaGestacionalAPartirDaDum() {
        // 20 semanas completas desde a DUM: a gestante esta na 21a semana.
        LocalDate dum = HOJE.minusWeeks(20);
        assertEquals(21, motor.calcularSemanaGestacional(dum, null, HOJE));
    }

    @Test
    void calculaSemanaGestacionalAPartirDaDppQuandoNaoHaDum() {
        // Faltam 10 semanas para a DPP de 40 semanas: gestacao na semana 30.
        LocalDate dpp = HOJE.plusWeeks(10);
        assertEquals(30, motor.calcularSemanaGestacional(null, dpp, HOJE));
    }

    @Test
    void semDumEDppNaoEstimaSemanaGestacional() {
        assertNull(motor.calcularSemanaGestacional(null, null, HOJE));
    }

    @Test
    void geraAlertaAltoParaItemObrigatorioComJanelaVencida() {
        LocalDate dum = HOJE.minusWeeks(29); // semana 30
        var alertas = motor.avaliar(
            requestComDum(dum, item(10L, "Hemograma", "PENDENTE", true, 1, 13)), HOJE);

        assertEquals(1, alertas.size());
        AlertaCalculado alerta = alertas.getFirst();
        assertEquals(TipoAlerta.PROCEDIMENTO_ATRASADO, alerta.tipo());
        assertEquals(PrioridadeAlerta.ALTA, alerta.prioridade());
        assertEquals(10L, alerta.origemId());
        assertTrue(alerta.mensagem().contains("atraso"));
    }

    @Test
    void itemNaoObrigatorioAtrasadoRecebePrioridadeMedia() {
        LocalDate dum = HOJE.minusWeeks(29); // semana 30
        var alertas = motor.avaliar(
            requestComDum(dum, item(11L, "Vacina opcional", "PENDENTE", false, 1, 13)), HOJE);

        assertEquals(PrioridadeAlerta.MEDIA, alertas.getFirst().prioridade());
    }

    @Test
    void geraAlertaMedioQuandoJanelaEstaEmCurso() {
        LocalDate dum = HOJE.minusWeeks(21); // semana 22
        var alertas = motor.avaliar(
            requestComDum(dum, item(12L, "Ultrassom morfologico", "PENDENTE", true, 20, 24)), HOJE);

        assertEquals(TipoAlerta.PROCEDIMENTO_PENDENTE, alertas.getFirst().tipo());
        assertEquals(PrioridadeAlerta.MEDIA, alertas.getFirst().prioridade());
    }

    @Test
    void geraAlertaBaixoQuandoJanelaComecaDentroDaAntecedenciaConfigurada() {
        LocalDate dum = HOJE.minusWeeks(17); // semana 18, janela comeca na 20
        var alertas = motor.avaliar(
            requestComDum(dum, item(13L, "Ultrassom morfologico", "PENDENTE", true, 20, 24)), HOJE);

        assertEquals(TipoAlerta.JANELA_PROXIMA, alertas.getFirst().tipo());
        assertEquals(PrioridadeAlerta.BAIXA, alertas.getFirst().prioridade());
    }

    @Test
    void naoAlertaQuandoJanelaEstaAlemDaAntecedenciaConfigurada() {
        LocalDate dum = HOJE.minusWeeks(9); // semana 10, janela comeca na 20
        var alertas = motor.avaliar(
            requestComDum(dum, item(14L, "Ultrassom morfologico", "PENDENTE", true, 20, 24)), HOJE);

        assertTrue(alertas.isEmpty());
    }

    @Test
    void antecedenciaConfiguravelAmpliaAJanelaPreventiva() {
        regras.setAntecedenciaSemanas(12);
        LocalDate dum = HOJE.minusWeeks(9); // semana 10, janela comeca na 20

        var alertas = motor.avaliar(
            requestComDum(dum, item(15L, "Ultrassom morfologico", "PENDENTE", true, 20, 24)), HOJE);

        assertEquals(TipoAlerta.JANELA_PROXIMA, alertas.getFirst().tipo());
    }

    @Test
    void itemRealizadoNaoGeraAlerta() {
        LocalDate dum = HOJE.minusWeeks(29);
        var alertas = motor.avaliar(
            requestComDum(dum, item(16L, "Hemograma", "REALIZADO", true, 1, 13)), HOJE);

        assertTrue(alertas.isEmpty());
    }

    @Test
    void itemMarcadoParaRevisaoGeraAlertaAltoMesmoSemDatasDaGestacao() {
        var request = new AvaliarChecklistRequest(1L, "Maria", null, null,
            List.of(item(17L, "Glicemia", "PRECISA_REVISAR", true, 1, 13)));

        var alertas = motor.avaliar(request, HOJE);

        assertEquals(TipoAlerta.REVISAO_SOLICITADA, alertas.getFirst().tipo());
        assertEquals(PrioridadeAlerta.ALTA, alertas.getFirst().prioridade());
    }

    @Test
    void semDatasDaGestacaoItensPendentesNaoGeramAlertaDeJanela() {
        var request = new AvaliarChecklistRequest(1L, "Maria", null, null,
            List.of(item(18L, "Hemograma", "PENDENTE", true, 1, 13)));

        assertTrue(motor.avaliar(request, HOJE).isEmpty());
    }

    @Test
    void desligarAlertaDeNaoObrigatoriosSuprimeApenasEsseGrupo() {
        regras.setAlertarNaoObrigatorios(false);
        LocalDate dum = HOJE.minusWeeks(29);

        var alertas = motor.avaliar(requestComDum(dum,
            item(19L, "Opcional", "PENDENTE", false, 1, 13),
            item(20L, "Obrigatorio", "PENDENTE", true, 1, 13)), HOJE);

        assertEquals(1, alertas.size());
        assertEquals(20L, alertas.getFirst().origemId());
    }

    @Test
    void ordenaAlertasDaMaiorParaAMenorPrioridade() {
        LocalDate dum = HOJE.minusWeeks(21); // semana 22
        var alertas = motor.avaliar(requestComDum(dum,
            item(30L, "Em curso", "PENDENTE", true, 20, 24),
            item(31L, "Revisar", "PRECISA_REVISAR", true, 20, 24),
            item(32L, "Em breve", "PENDENTE", true, 24, 28)), HOJE);

        assertEquals(3, alertas.size());
        assertEquals(PrioridadeAlerta.ALTA, alertas.get(0).prioridade());
        assertEquals(PrioridadeAlerta.MEDIA, alertas.get(1).prioridade());
        assertEquals(PrioridadeAlerta.BAIXA, alertas.get(2).prioridade());
    }

    @Test
    void converteSemanaRecomendadaEmDataDeReferenciaQuandoHaDum() {
        LocalDate dum = HOJE.minusWeeks(21);
        var alertas = motor.avaliar(
            requestComDum(dum, item(40L, "Ultrassom", "PENDENTE", true, 20, 24)), HOJE);

        assertEquals(dum.plusWeeks(19), alertas.getFirst().dataReferencia());
    }

    @Test
    void semanaGestacionalNuncaExcedeOsLimitesFisiologicos() {
        assertEquals(42, motor.calcularSemanaGestacional(HOJE.minusWeeks(60), null, HOJE));
        assertEquals(1, motor.calcularSemanaGestacional(HOJE.plusWeeks(5), null, HOJE));
    }
}
