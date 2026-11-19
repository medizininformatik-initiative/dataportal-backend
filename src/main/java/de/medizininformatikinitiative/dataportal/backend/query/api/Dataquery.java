package de.medizininformatikinitiative.dataportal.backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.validation.Valid;
import lombok.Builder;

import java.sql.Timestamp;

@JsonInclude(Include.NON_EMPTY)
@Builder
public record Dataquery(
    @JsonProperty long id,
    @Valid @JsonProperty Crtdl content,
    @JsonProperty String label,
    @JsonProperty String comment,
    @JsonProperty String lastModified,
    @JsonProperty String createdBy,
    @JsonProperty CrtdlSectionInfo ccdl,
    @JsonProperty CrtdlSectionInfo dataExtraction,
    @JsonProperty Long resultSize,
    @JsonProperty Timestamp expiresAt
) {
  private static ObjectMapper jsonUtil = JsonMapper.builderWithJackson2Defaults().build();

  public static Dataquery of(de.medizininformatikinitiative.dataportal.backend.query.persistence.DataqueryEntity in) throws JacksonException {
    return Dataquery.builder()
        .id(in.getId())
        .label(in.getLabel())
        .comment(in.getComment())
        .createdBy(in.getCreatedBy())
        .resultSize(in.getResultSize())
        .lastModified(in.getLastModified() == null ? null : in.getLastModified().toString())
        .content(parseCrtdl(in.getCrtdl()))
        .expiresAt(in.getExpiresAt())
        .build();
  }

  /**
   * Parses the stored crtdl string. Older entries may contain a bare CCDL instead of a CRTDL. Those are wrapped
   * into a CRTDL (the same way the V10 migration did) so that they can still be read.
   *
   * @param crtdl the json string as stored in the database, may be null
   * @return the parsed {@link Crtdl} or null if nothing is stored
   * @throws JacksonException if the stored string can not be parsed
   */
  private static Crtdl parseCrtdl(String crtdl) throws JacksonException {
    if (crtdl == null || crtdl.isBlank()) {
      return null;
    }
    JsonNode node = jsonUtil.readTree(crtdl);
    if (isBareCcdl(node)) {
      return Crtdl.builder()
          .display("")
          .cohortDefinition(jsonUtil.treeToValue(node, Ccdl.class))
          .build();
    }
    return jsonUtil.treeToValue(node, Crtdl.class);
  }

  private static boolean isBareCcdl(JsonNode node) {
    return node.isObject()
        && !node.has("cohortDefinition")
        && !node.has("dataExtraction")
        && (node.has("inclusionCriteria") || node.has("exclusionCriteria"));
  }
}
