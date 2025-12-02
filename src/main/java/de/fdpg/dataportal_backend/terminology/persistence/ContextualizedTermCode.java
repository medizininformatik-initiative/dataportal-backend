package de.fdpg.dataportal_backend.terminology.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Table(name = "contextualized_termcode", schema = "public")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class ContextualizedTermCode {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "context_termcode_hash", nullable = false, length = -1)
    private String contextTermcodeHash;
    @Basic
    @Column(name = "context_id", nullable = false)
    private int contextId;
    @Basic
    @Column(name = "termcode_id", nullable = false)
    private int termCodeId;
    @Basic
    @Column(name = "ui_profile_id", nullable = true)
    private Integer uiProfileId;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ContextualizedTermCode that = (ContextualizedTermCode) o;
        return getContextTermcodeHash() != null && Objects.equals(getContextTermcodeHash(), that.getContextTermcodeHash());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
