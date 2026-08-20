package estevezalvarez.gestarafeto.alertas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Alerta derivado do checklist de uma gestante.
 *
 * <p>Este microsservico nao compartilha banco com o servico principal: {@code gestanteId} e
 * {@code origemId} (id do item de checklist) sao referencias logicas, sem chave estrangeira.
 * O nome da gestante e replicado no momento da avaliacao para que a listagem de alertas
 * nao dependa de uma chamada de volta ao servico principal.</p>
 */
@Entity
@Table(
    name = "alertas",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_alerta_origem_tipo",
        columnNames = {"gestante_id", "origem_id", "tipo"}
    ),
    indexes = {
        @Index(name = "idx_alertas_gestante", columnList = "gestante_id"),
        @Index(name = "idx_alertas_gestante_status", columnList = "gestante_id,status")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @NotNull
    @Column(name = "gestante_id", nullable = false)
    private Long gestanteId;

    @NotBlank
    @Column(name = "gestante_nome", nullable = false, length = 160)
    private String gestanteNome;

    /** Id do item de checklist no servico principal que originou o alerta. */
    @NotNull
    @Column(name = "origem_id", nullable = false)
    private Long origemId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoAlerta tipo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrioridadeAlerta prioridade;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAlerta status;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    /** Data estimada em que o procedimento deveria ocorrer, quando calculavel. */
    @Column(name = "data_referencia")
    private LocalDate dataReferencia;

    @Column(name = "semana_gestacional_referencia")
    private Integer semanaGestacionalReferencia;

    @CreatedDate
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @Column(name = "data_leitura")
    private LocalDateTime dataLeitura;

    @Column(name = "data_resolucao")
    private LocalDateTime dataResolucao;

    @PrePersist
    private void prePersist() {
        if (status == null) {
            status = StatusAlerta.ABERTO;
        }
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }

    @PreUpdate
    private void preUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    public void marcarLido() {
        if (status == StatusAlerta.ABERTO) {
            status = StatusAlerta.LIDO;
            dataLeitura = LocalDateTime.now();
        }
    }

    public void resolver() {
        if (status.isAtivo()) {
            status = StatusAlerta.RESOLVIDO;
            dataResolucao = LocalDateTime.now();
        }
    }

    public void cancelar() {
        if (status.isAtivo()) {
            status = StatusAlerta.CANCELADO;
            dataResolucao = LocalDateTime.now();
        }
    }

    /** Reabre um alerta encerrado quando a condicao que o originou volta a ocorrer. */
    public void reabrir() {
        if (!status.isAtivo()) {
            status = StatusAlerta.ABERTO;
            dataLeitura = null;
            dataResolucao = null;
        }
    }
}
