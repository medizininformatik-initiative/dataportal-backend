package de.numcodex.feasibility_gui_backend.query.persistence;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Dataquery {

  private static ObjectMapper jsonUtil = new ObjectMapper().findAndRegisterModules();

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_by", updatable = false)
  private String createdBy;

  @Column(name = "label", nullable = false)
  private String label;

  @Column(name = "comment")
  private String comment;

  @Column(name = "crtdl")
  private String crtdl;

  @Column(name = "last_modified", insertable = false)
  private Timestamp lastModified;

  @Column(name = "result_size")
  private Long resultSize;

  @Column(name = "expires_at")
  private Timestamp expiresAt;

  public static Dataquery of(de.numcodex.feasibility_gui_backend.query.api.Dataquery in) throws JacksonException {
    var out = new de.numcodex.feasibility_gui_backend.query.persistence.Dataquery();
    out.setId(in.id() > 0 ? in.id() : null);
    out.setLabel(in.label());
    out.setComment(in.comment());
    if (in.lastModified() != null) {
      out.setLastModified(Timestamp.valueOf(in.lastModified()));
    }
    out.setCreatedBy(in.createdBy());
    out.setResultSize(in.resultSize());
    out.setCrtdl(jsonUtil.writeValueAsString(in.content()));
    out.setExpiresAt(in.expiresAt());
    return out;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Dataquery dataquery = (Dataquery) o;
    return Objects.equals(id, dataquery.id) && Objects.equals(createdBy, dataquery.createdBy) && Objects.equals(label, dataquery.label) && Objects.equals(comment, dataquery.comment) && Objects.equals(crtdl, dataquery.crtdl) && Objects.equals(lastModified, dataquery.lastModified) && Objects.equals(resultSize, dataquery.resultSize) && Objects.equals(expiresAt, dataquery.expiresAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, createdBy, label, comment, crtdl, lastModified, resultSize, expiresAt);
  }
}
