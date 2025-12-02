package de.fdpg.dataportal_backend.terminology.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Context {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private Long id;
    @Basic
    @Column(name = "system", nullable = false, length = -1)
    private String system;
    @Basic
    @Column(name = "code", nullable = false, length = -1)
    private String code;
    @Basic
    @Column(name = "version", nullable = true, length = -1)
    private String version;
    @Basic
    @Column(name = "display", nullable = false, length = -1)
    private String display;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Context context = (Context) o;
        return getId() != null && Objects.equals(getId(), context.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, system, code, version, display);
    }
}
