package estevezalvarez.GestarAfeto.checklist.domain;

import estevezalvarez.GestarAfeto.gestante.domain.Gestante;
import estevezalvarez.GestarAfeto.procedimento.domain.ProcedimentoPreNatal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Check(constraints = "status <> 'REALIZADO' or data_realizacao is not null")
@Table(
    name = "itens_checklist_gestante",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_checklist_gestante_procedimento",
        columnNames = {"gestante_id", "procedimento_id"}
    ),
    indexes = {
        @Index(name = "idx_checklist_gestante", columnList = "gestante_id"),
        @Index(name = "idx_checklist_gestante_status", columnList = "gestante_id,status")
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
public class ItemChecklistGestante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gestante_id", nullable = false)
    private Gestante gestante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedimento_id", nullable = false)
    private ProcedimentoPreNatal procedimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusChecklist status;

    @Column(name = "data_prevista")
    private LocalDate dataPrevista;

    @Column(name = "data_realizacao")
    private LocalDate dataRealizacao;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @CreatedDate
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    private void prePersist() {
        if (status == null) {
            status = StatusChecklist.PENDENTE;
        }
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }

    @PreUpdate
    private void preUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}
