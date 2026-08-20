package estevezalvarez.GestarAfeto.procedimento.repository;

import estevezalvarez.GestarAfeto.procedimento.domain.ProcedimentoPreNatal;
import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;
import estevezalvarez.GestarAfeto.procedimento.domain.TrimestreGestacional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcedimentoRepository extends JpaRepository<ProcedimentoPreNatal, Long> {
    List<ProcedimentoPreNatal> findByAtivoTrue();
    Page<ProcedimentoPreNatal> findByAtivoTrue(Pageable pageable);
    List<ProcedimentoPreNatal> findByAtivoTrueOrderByNomeAsc();
    Page<ProcedimentoPreNatal> findByTipoAndAtivoTrue(TipoProcedimento tipo, Pageable pageable);
    Page<ProcedimentoPreNatal> findByTrimestreRecomendadoAndAtivoTrue(TrimestreGestacional trimestre, Pageable pageable);
    Page<ProcedimentoPreNatal> findByNomeContainingIgnoreCaseAndAtivoTrue(String nome, Pageable pageable);

    @Query("""
        select p from ProcedimentoPreNatal p
        where p.ativo = true
          and (p.semanaInicialRecomendada is null or p.semanaInicialRecomendada <= :semana)
          and (p.semanaFinalRecomendada is null or p.semanaFinalRecomendada >= :semana)
        order by p.nome asc
        """)
    List<ProcedimentoPreNatal> findAtivosRecomendadosNaSemana(@Param("semana") Integer semana);
}
