package de.fdpg.dataportal_backend.terminology.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.Objects;

@Entity(name = "UI_PROFILE")
@Table(name = "UI_PROFILE_TABLE")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@IdClass(Coding.class)
public class UiProfileContent implements Serializable {
  @Id
  @Column(name = "system")
  private String system;

  @Id
  @Column(name = "code")
  private String code;

  @Id
  @Column(name = "version")
  private String version;

  @Column(name = "UI_Profile", nullable = false)
  private String uiProfile;

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
    Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) return false;
    UiProfileContent that = (UiProfileContent) o;
    return getSystem() != null && Objects.equals(getSystem(), that.getSystem())
        && getCode() != null && Objects.equals(getCode(), that.getCode())
        && getVersion() != null && Objects.equals(getVersion(), that.getVersion());
  }

  @Override
  public final int hashCode() {
    return Objects.hash(system, code, version);
  }
}
