package de.fdpg.dataportal_backend.query.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class QueryDispatch {

    @EmbeddedId
    private QueryDispatchId id;

    @MapsId("queryId")
    @JoinColumn(referencedColumnName = "id", name = "query_id", nullable = false)
    @ManyToOne(fetch = LAZY)
    @ToString.Exclude
    private Query query;

    @Column(name = "dispatched_at", insertable = false, updatable = false)
    private Timestamp dispatchedAt;

    @Data
    @Embeddable
    public static class QueryDispatchId implements Serializable {
        @Column(name = "query_id")
        private Long queryId;

        @Column(name = "external_query_id")
        private String externalId;

        @Convert(converter = BrokerTypeConverter.class)
        @Column(name = "broker_type")
        private BrokerClientType brokerType;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        QueryDispatch that = (QueryDispatch) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
