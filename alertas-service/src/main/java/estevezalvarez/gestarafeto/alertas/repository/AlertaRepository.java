package estevezalvarez.gestarafeto.alertas.repository;

import estevezalvarez.gestarafeto.alertas.domain.Alerta;
import estevezalvarez.gestarafeto.alertas.domain.PrioridadeAlerta;
import estevezalvarez.gestarafeto.alertas.domain.StatusAlerta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio dedicado do microsservico. Opera exclusivamente sobre a base de alertas,
 * sem qualquer join com as tabelas do servico principal.
 */
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findByGestanteId(Long gestanteId);

    Page<Alerta> findByGestanteId(Long gestanteId, Pageable pageable);

    Page<Alerta> findByGestanteIdAndStatus(Long gestanteId, StatusAlerta status, Pageable pageable);

    Page<Alerta> findByGestanteIdAndPrioridade(Long gestanteId, PrioridadeAlerta prioridade, Pageable pageable);

    Page<Alerta> findByStatus(StatusAlerta status, Pageable pageable);

    /**
     * Alertas ativos ordenados por severidade real.
     *
     * <p>A prioridade e persistida como texto ({@code EnumType.STRING}), entao um
     * {@code order by prioridade desc} ordenaria alfabeticamente (ALTA, BAIXA, MEDIA).
     * O {@code case} traduz cada valor para o peso do enum e mantem a ordenacao no banco.</p>
     */
    @Query("""
        select a
        from Alerta a
        where a.gestanteId = :gestanteId and a.status in :status
        order by
            case a.prioridade
                when estevezalvarez.gestarafeto.alertas.domain.PrioridadeAlerta.ALTA then 3
                when estevezalvarez.gestarafeto.alertas.domain.PrioridadeAlerta.MEDIA then 2
                else 1
            end desc,
            a.dataReferencia asc nulls last,
            a.id asc
        """)
    List<Alerta> findAtivosOrdenadosPorSeveridade(
        @Param("gestanteId") Long gestanteId,
        @Param("status") List<StatusAlerta> status);

    long countByGestanteIdAndStatus(Long gestanteId, StatusAlerta status);

    long countByGestanteIdAndStatusInAndPrioridade(
        Long gestanteId, List<StatusAlerta> status, PrioridadeAlerta prioridade);

    long deleteByGestanteId(Long gestanteId);

    /**
     * Contagem de alertas ativos agrupada por prioridade, usada pelo painel do front-end.
     * Evita trazer todos os alertas para memoria so para contar.
     */
    @Query("""
        select a.prioridade, count(a)
        from Alerta a
        where a.gestanteId = :gestanteId and a.status in :status
        group by a.prioridade
        """)
    List<Object[]> contarAtivosPorPrioridade(
        @Param("gestanteId") Long gestanteId,
        @Param("status") List<StatusAlerta> status);
}
