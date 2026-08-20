package estevezalvarez.gestarafeto.alertas.service;

import estevezalvarez.gestarafeto.alertas.domain.Alerta;
import estevezalvarez.gestarafeto.alertas.domain.PrioridadeAlerta;
import estevezalvarez.gestarafeto.alertas.domain.StatusAlerta;
import estevezalvarez.gestarafeto.alertas.dto.AlertaResponse;
import estevezalvarez.gestarafeto.alertas.dto.AvaliarChecklistRequest;
import estevezalvarez.gestarafeto.alertas.dto.ResumoAlertasResponse;
import estevezalvarez.gestarafeto.alertas.exception.RecursoNaoEncontradoException;
import estevezalvarez.gestarafeto.alertas.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private static final List<StatusAlerta> ATIVOS = List.of(StatusAlerta.ABERTO, StatusAlerta.LIDO);

    private final AlertaRepository alertaRepository;
    private final MotorDeRegrasAlerta motor;

    /**
     * Reconcilia os alertas de uma gestante com o estado atual do checklist.
     *
     * <p>A operacao e idempotente: chamar duas vezes com o mesmo snapshot produz o mesmo
     * resultado, sem duplicar registros. Alertas cuja condicao deixou de existir sao
     * marcados como RESOLVIDO em vez de apagados, preservando o rastro do que ja foi
     * sinalizado a equipe.</p>
     */
    @Transactional
    public List<AlertaResponse> avaliar(AvaliarChecklistRequest request) {
        LocalDate hoje = LocalDate.now();
        List<AlertaCalculado> calculados = motor.avaliar(request, hoje);

        Map<String, Alerta> existentes = new HashMap<>();
        for (Alerta alerta : alertaRepository.findByGestanteId(request.gestanteId())) {
            existentes.put(alerta.getOrigemId() + "|" + alerta.getTipo().name(), alerta);
        }

        Map<String, AlertaCalculado> desejados = new LinkedHashMap<>();
        for (AlertaCalculado calculado : calculados) {
            desejados.put(calculado.chave(), calculado);
        }

        List<Alerta> paraSalvar = new ArrayList<>();

        for (Map.Entry<String, AlertaCalculado> entry : desejados.entrySet()) {
            AlertaCalculado calculado = entry.getValue();
            Alerta existente = existentes.get(entry.getKey());
            if (existente == null) {
                paraSalvar.add(novoAlerta(request, calculado));
            } else {
                atualizar(existente, request, calculado);
                paraSalvar.add(existente);
            }
        }

        for (Map.Entry<String, Alerta> entry : existentes.entrySet()) {
            Alerta alerta = entry.getValue();
            if (!desejados.containsKey(entry.getKey()) && alerta.getStatus().isAtivo()) {
                alerta.resolver();
                paraSalvar.add(alerta);
            }
        }

        alertaRepository.saveAll(paraSalvar);
        return listarAtivos(request.gestanteId());
    }

    private Alerta novoAlerta(AvaliarChecklistRequest request, AlertaCalculado calculado) {
        return Alerta.builder()
            .gestanteId(request.gestanteId())
            .gestanteNome(request.gestanteNome())
            .origemId(calculado.origemId())
            .tipo(calculado.tipo())
            .prioridade(calculado.prioridade())
            .status(StatusAlerta.ABERTO)
            .titulo(calculado.titulo())
            .mensagem(calculado.mensagem())
            .dataReferencia(calculado.dataReferencia())
            .semanaGestacionalReferencia(calculado.semanaGestacionalReferencia())
            .build();
    }

    /** Mantem o status de leitura ja registrado pela equipe, atualizando apenas o conteudo. */
    private void atualizar(Alerta alerta, AvaliarChecklistRequest request, AlertaCalculado calculado) {
        alerta.reabrir();
        alerta.setGestanteNome(request.gestanteNome());
        alerta.setPrioridade(calculado.prioridade());
        alerta.setTitulo(calculado.titulo());
        alerta.setMensagem(calculado.mensagem());
        alerta.setDataReferencia(calculado.dataReferencia());
        alerta.setSemanaGestacionalReferencia(calculado.semanaGestacionalReferencia());
    }

    @Transactional(readOnly = true)
    public List<AlertaResponse> listarAtivos(Long gestanteId) {
        return alertaRepository
            .findAtivosOrdenadosPorSeveridade(gestanteId, ATIVOS)
            .stream()
            .map(AlertaResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<AlertaResponse> listarPaginado(
            Long gestanteId, StatusAlerta status, PrioridadeAlerta prioridade, Pageable pageable) {
        if (gestanteId == null) {
            Page<Alerta> pagina = status == null
                ? alertaRepository.findAll(pageable)
                : alertaRepository.findByStatus(status, pageable);
            return pagina.map(AlertaResponse::from);
        }
        if (status != null) {
            return alertaRepository.findByGestanteIdAndStatus(gestanteId, status, pageable)
                .map(AlertaResponse::from);
        }
        if (prioridade != null) {
            return alertaRepository.findByGestanteIdAndPrioridade(gestanteId, prioridade, pageable)
                .map(AlertaResponse::from);
        }
        return alertaRepository.findByGestanteId(gestanteId, pageable).map(AlertaResponse::from);
    }

    @Transactional(readOnly = true)
    public AlertaResponse buscarPorId(Long id) {
        return AlertaResponse.from(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public ResumoAlertasResponse resumo(Long gestanteId) {
        Map<PrioridadeAlerta, Long> porPrioridade = new HashMap<>();
        for (Object[] linha : alertaRepository.contarAtivosPorPrioridade(gestanteId, ATIVOS)) {
            porPrioridade.put((PrioridadeAlerta) linha[0], (Long) linha[1]);
        }
        long alta = porPrioridade.getOrDefault(PrioridadeAlerta.ALTA, 0L);
        long media = porPrioridade.getOrDefault(PrioridadeAlerta.MEDIA, 0L);
        long baixa = porPrioridade.getOrDefault(PrioridadeAlerta.BAIXA, 0L);

        return new ResumoAlertasResponse(
            gestanteId,
            alta + media + baixa,
            alta,
            media,
            baixa,
            alertaRepository.countByGestanteIdAndStatus(gestanteId, StatusAlerta.ABERTO),
            alertaRepository.countByGestanteIdAndStatus(gestanteId, StatusAlerta.RESOLVIDO));
    }

    @Transactional
    public AlertaResponse marcarLido(Long id) {
        Alerta alerta = buscarEntidade(id);
        alerta.marcarLido();
        return AlertaResponse.from(alertaRepository.save(alerta));
    }

    @Transactional
    public AlertaResponse resolver(Long id) {
        Alerta alerta = buscarEntidade(id);
        alerta.resolver();
        return AlertaResponse.from(alertaRepository.save(alerta));
    }

    @Transactional
    public AlertaResponse cancelar(Long id) {
        Alerta alerta = buscarEntidade(id);
        alerta.cancelar();
        return AlertaResponse.from(alertaRepository.save(alerta));
    }

    /** Chamado pelo servico principal quando a gestante e removida do sistema. */
    @Transactional
    public long removerPorGestante(Long gestanteId) {
        return alertaRepository.deleteByGestanteId(gestanteId);
    }

    private Alerta buscarEntidade(Long id) {
        return alertaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta nao encontrado com id: " + id));
    }
}
