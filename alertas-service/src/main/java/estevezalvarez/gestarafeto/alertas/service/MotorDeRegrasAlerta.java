package estevezalvarez.gestarafeto.alertas.service;

import estevezalvarez.gestarafeto.alertas.config.AlertaRegrasProperties;
import estevezalvarez.gestarafeto.alertas.domain.PrioridadeAlerta;
import estevezalvarez.gestarafeto.alertas.domain.TipoAlerta;
import estevezalvarez.gestarafeto.alertas.dto.AvaliarChecklistRequest;
import estevezalvarez.gestarafeto.alertas.dto.ItemChecklistSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Regra de negocio exclusiva do microsservico: transforma o checklist de uma gestante
 * em uma lista priorizada de alertas.
 *
 * <p>E deliberadamente sem estado e sem acesso a banco, o que o torna testavel de forma
 * isolada e reutilizavel caso o microsservico ganhe outros canais de entrada.</p>
 */
@Component
@RequiredArgsConstructor
public class MotorDeRegrasAlerta {

    private static final String STATUS_PENDENTE = "PENDENTE";
    private static final String STATUS_PRECISA_REVISAR = "PRECISA_REVISAR";

    private final AlertaRegrasProperties regras;

    /**
     * Estima a semana gestacional atual.
     *
     * <p>Prioriza a DUM (medida direta). Sem DUM, deriva da DPP considerando uma gestacao
     * de {@code semanasGestacaoTotal} semanas. Retorna {@code null} quando nenhuma das duas
     * datas foi informada — nesse caso as regras baseadas em janela nao se aplicam.</p>
     */
    public Integer calcularSemanaGestacional(LocalDate dum, LocalDate dpp, LocalDate hoje) {
        if (dum != null) {
            long semanas = ChronoUnit.WEEKS.between(dum, hoje);
            return normalizar(semanas + 1);
        }
        if (dpp != null) {
            long semanasRestantes = ChronoUnit.WEEKS.between(hoje, dpp);
            return normalizar(regras.getSemanasGestacaoTotal() - semanasRestantes);
        }
        return null;
    }

    private Integer normalizar(long semana) {
        if (semana < 1) {
            return 1;
        }
        if (semana > 42) {
            return 42;
        }
        return (int) semana;
    }

    public List<AlertaCalculado> avaliar(AvaliarChecklistRequest request, LocalDate hoje) {
        Integer semanaAtual = calcularSemanaGestacional(
            request.dataUltimaMenstruacao(), request.dataProvavelParto(), hoje);

        List<AlertaCalculado> alertas = new ArrayList<>();
        for (ItemChecklistSnapshot item : request.itens()) {
            AlertaCalculado alerta = avaliarItem(item, semanaAtual, request.dataUltimaMenstruacao());
            if (alerta != null) {
                alertas.add(alerta);
            }
        }
        alertas.sort(
            java.util.Comparator
                .comparingInt((AlertaCalculado a) -> a.prioridade().getPeso()).reversed()
                .thenComparing(AlertaCalculado::origemId));
        return alertas;
    }

    private AlertaCalculado avaliarItem(ItemChecklistSnapshot item, Integer semanaAtual, LocalDate dum) {
        String status = item.status() == null ? "" : item.status().trim().toUpperCase();

        if (STATUS_PRECISA_REVISAR.equals(status)) {
            return new AlertaCalculado(
                item.itemId(),
                TipoAlerta.REVISAO_SOLICITADA,
                PrioridadeAlerta.ALTA,
                "Revisao solicitada: " + item.procedimentoNome(),
                "O item \"" + item.procedimentoNome() + "\" foi marcado para revisao no checklist"
                    + " e aguarda conferencia da equipe.",
                dataReferencia(dum, item.semanaInicialRecomendada()),
                item.semanaInicialRecomendada());
        }

        // Itens realizados ou nao aplicaveis nao geram alerta; alertas anteriores sao resolvidos
        // pelo AlertaService durante a reconciliacao.
        if (!STATUS_PENDENTE.equals(status)) {
            return null;
        }
        if (!item.obrigatorio() && !regras.isAlertarNaoObrigatorios()) {
            return null;
        }
        if (semanaAtual == null) {
            return null;
        }

        Integer inicio = item.semanaInicialRecomendada();
        Integer fim = item.semanaFinalRecomendada();

        if (fim != null && semanaAtual > fim) {
            int atraso = semanaAtual - fim;
            return new AlertaCalculado(
                item.itemId(),
                TipoAlerta.PROCEDIMENTO_ATRASADO,
                item.obrigatorio() ? PrioridadeAlerta.ALTA : PrioridadeAlerta.MEDIA,
                "Atrasado: " + item.procedimentoNome(),
                "A janela recomendada terminou na semana " + fim + " e a gestacao esta na semana "
                    + semanaAtual + " (" + atraso + " semana(s) de atraso).",
                dataReferencia(dum, fim),
                fim);
        }

        if (dentroDaJanela(semanaAtual, inicio, fim)) {
            return new AlertaCalculado(
                item.itemId(),
                TipoAlerta.PROCEDIMENTO_PENDENTE,
                item.obrigatorio() ? PrioridadeAlerta.MEDIA : PrioridadeAlerta.BAIXA,
                "Pendente agora: " + item.procedimentoNome(),
                "A janela recomendada (semanas " + descreverJanela(inicio, fim)
                    + ") esta em curso e o item continua pendente.",
                dataReferencia(dum, inicio),
                inicio);
        }

        if (inicio != null && semanaAtual < inicio
                && inicio - semanaAtual <= regras.getAntecedenciaSemanas()) {
            return new AlertaCalculado(
                item.itemId(),
                TipoAlerta.JANELA_PROXIMA,
                PrioridadeAlerta.BAIXA,
                "Em breve: " + item.procedimentoNome(),
                "A janela recomendada comeca na semana " + inicio + ", em "
                    + (inicio - semanaAtual) + " semana(s).",
                dataReferencia(dum, inicio),
                inicio);
        }

        return null;
    }

    private boolean dentroDaJanela(int semanaAtual, Integer inicio, Integer fim) {
        if (inicio == null && fim == null) {
            return false;
        }
        boolean depoisDoInicio = inicio == null || semanaAtual >= inicio;
        boolean antesDoFim = fim == null || semanaAtual <= fim;
        return depoisDoInicio && antesDoFim;
    }

    private String descreverJanela(Integer inicio, Integer fim) {
        if (inicio != null && fim != null) {
            return inicio + " a " + fim;
        }
        return inicio != null ? "a partir de " + inicio : "ate " + fim;
    }

    /** Converte uma semana gestacional em data de calendario, quando a DUM e conhecida. */
    private LocalDate dataReferencia(LocalDate dum, Integer semana) {
        if (dum == null || semana == null) {
            return null;
        }
        return dum.plusWeeks(semana - 1L);
    }
}
