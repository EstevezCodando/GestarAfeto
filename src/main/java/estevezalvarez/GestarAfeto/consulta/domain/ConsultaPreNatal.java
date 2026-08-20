package estevezalvarez.GestarAfeto.consulta.domain;

import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Check(constraints = "(semana_gestacional is null or semana_gestacional between 1 and 42) and (peso is null or peso > 0)")
@Table(
    name = "consultas_prenatal",
    indexes = @Index(name = "idx_consultas_gestante_data_desc", columnList = "gestante_id,data_consulta DESC")
)
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConsultaPreNatal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gestante_id", nullable = false)
    private Gestante gestante;

    @NotNull
    @Column(name = "data_consulta", nullable = false)
    private LocalDate dataConsulta;

    @Column(name = "semana_gestacional")
    private Integer semanaGestacional;

    @Column(precision = 5, scale = 2)
    private BigDecimal peso;

    @Column(name = "pressao_arterial", length = 20)
    private String pressaoArterial;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @CreatedDate
    @Column(name = "data_registro", nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    @PrePersist
    private void prePersist() {
        if (dataRegistro == null) {
            dataRegistro = LocalDateTime.now();
        }
    }
}
