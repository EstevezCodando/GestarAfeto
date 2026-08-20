package estevezalvarez.GestarAfeto.auditoria.service;

import estevezalvarez.GestarAfeto.auditoria.domain.AuditoriaRevision;
import estevezalvarez.GestarAfeto.auditoria.dto.AuditoriaRevisionResponse;
import estevezalvarez.GestarAfeto.checklist.domain.ItemChecklistGestante;
import estevezalvarez.GestarAfeto.consulta.domain.ConsultaPreNatal;
import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import estevezalvarez.GestarAfeto.procedimento.domain.ProcedimentoPreNatal;
import estevezalvarez.GestarAfeto.shared.exception.RegraDeNegocioException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<AuditoriaRevisionResponse> historico(String entidade, Long id) {
        Class<?> entityClass = resolverEntidade(entidade);
        return AuditReaderFactory.get(entityManager)
            .createQuery()
            .forRevisionsOfEntity(entityClass, false, true)
            .add(org.hibernate.envers.query.AuditEntity.id().eq(id))
            .getResultList()
            .stream()
            .map(this::mapearResultado)
            .toList();
    }

    private Class<?> resolverEntidade(String entidade) {
        return switch (entidade.toLowerCase()) {
            case "gestante", "gestantes" -> Gestante.class;
            case "procedimento", "procedimentos" -> ProcedimentoPreNatal.class;
            case "checklist", "item-checklist", "item_checklist" -> ItemChecklistGestante.class;
            case "consulta", "consultas" -> ConsultaPreNatal.class;
            default -> throw new RegraDeNegocioException("Entidade nao auditada: " + entidade);
        };
    }

    private AuditoriaRevisionResponse mapearResultado(Object resultado) {
        Object[] linha = (Object[]) resultado;
        Object entidade = linha[0];
        AuditoriaRevision revisao = (AuditoriaRevision) linha[1];
        RevisionType tipo = (RevisionType) linha[2];

        return new AuditoriaRevisionResponse(
            revisao.getId(),
            Instant.ofEpochMilli(revisao.getTimestamp()),
            tipo.name(),
            snapshot(entidade)
        );
    }

    private Map<String, Object> snapshot(Object entidade) {
        Map<String, Object> dados = new LinkedHashMap<>();
        if (entidade instanceof Gestante gestante) {
            dados.put("id", gestante.getId());
            dados.put("nome", gestante.getNome());
            dados.put("email", gestante.getEmail());
            dados.put("telefone", gestante.getTelefone());
            dados.put("dataUltimaMenstruacao", gestante.getDataUltimaMenstruacao());
            dados.put("dataProvavelParto", gestante.getDataProvavelParto());
            dados.put("observacoes", gestante.getObservacoes());
            return dados;
        }
        if (entidade instanceof ProcedimentoPreNatal procedimento) {
            dados.put("id", procedimento.getId());
            dados.put("nome", procedimento.getNome());
            dados.put("tipo", procedimento.getTipo());
            dados.put("trimestreRecomendado", procedimento.getTrimestreRecomendado());
            dados.put("semanaInicialRecomendada", procedimento.getSemanaInicialRecomendada());
            dados.put("semanaFinalRecomendada", procedimento.getSemanaFinalRecomendada());
            dados.put("obrigatorio", procedimento.isObrigatorio());
            dados.put("ativo", procedimento.isAtivo());
            return dados;
        }
        if (entidade instanceof ItemChecklistGestante item) {
            dados.put("id", item.getId());
            dados.put("gestanteId", item.getGestante() == null ? null : item.getGestante().getId());
            dados.put("procedimentoId", item.getProcedimento() == null ? null : item.getProcedimento().getId());
            dados.put("status", item.getStatus());
            dados.put("dataPrevista", item.getDataPrevista());
            dados.put("dataRealizacao", item.getDataRealizacao());
            dados.put("observacao", item.getObservacao());
            return dados;
        }
        if (entidade instanceof ConsultaPreNatal consulta) {
            dados.put("id", consulta.getId());
            dados.put("gestanteId", consulta.getGestante() == null ? null : consulta.getGestante().getId());
            dados.put("dataConsulta", consulta.getDataConsulta());
            dados.put("semanaGestacional", consulta.getSemanaGestacional());
            dados.put("peso", consulta.getPeso());
            dados.put("pressaoArterial", consulta.getPressaoArterial());
            dados.put("observacoes", consulta.getObservacoes());
            return dados;
        }
        return dados;
    }
}
