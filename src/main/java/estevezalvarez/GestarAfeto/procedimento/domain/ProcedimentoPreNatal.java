package estevezalvarez.GestarAfeto.procedimento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Check(constraints = """
    (semana_inicial_recomendada is null or semana_inicial_recomendada between 1 and 42)
    and (semana_final_recomendada is null or semana_final_recomendada between 1 and 42)
    and (
        semana_inicial_recomendada is null
        or semana_final_recomendada is null
        or semana_inicial_recomendada <= semana_final_recomendada
    )
    """)
@Table(
    name = "procedimentos_prenatal",
    indexes = {
        @Index(name = "idx_procedimentos_ativo", columnList = "ativo"),
        @Index(name = "idx_procedimentos_ativo_tipo", columnList = "ativo,tipo")
    }
)
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProcedimentoPreNatal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @NotBlank
    @Column(nullable = false, length = 180)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoProcedimento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "trimestre_recomendado", length = 20)
    private TrimestreGestacional trimestreRecomendado;

    @Column(name = "semana_inicial_recomendada")
    private Integer semanaInicialRecomendada;

    @Column(name = "semana_final_recomendada")
    private Integer semanaFinalRecomendada;

    @Column(nullable = false)
    private boolean obrigatorio = true;

    @Column(nullable = false)
    private boolean ativo = true;
}
