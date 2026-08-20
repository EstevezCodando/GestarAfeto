package estevezalvarez.GestarAfeto.auditoria.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Table(name = "revinfo")
@RevisionEntity
public class AuditoriaRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private Integer id;

    @RevisionTimestamp
    @Column(name = "revtstmp", nullable = false)
    private Long timestamp;

    public Integer getId() {
        return id;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
