package de.fdpg.dataportal_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;

import java.sql.Timestamp;

@JsonInclude(Include.NON_EMPTY)
@Builder
public record Dataquery(
    @JsonProperty long id,
    @JsonProperty Crtdl content,
    @JsonProperty String label,
    @JsonProperty String comment,
    @JsonProperty String lastModified,
    @JsonProperty String createdBy,
    @JsonProperty CrtdlSectionInfo ccdl,
    @JsonProperty CrtdlSectionInfo dataExtraction,
    @JsonProperty Long resultSize,
    @JsonProperty Timestamp expiresAt
) {
  private static ObjectMapper jsonUtil = new ObjectMapper().findAndRegisterModules();

  public static Dataquery of(de.fdpg.dataportal_backend.query.persistence.Dataquery in) throws JsonProcessingException {
    return Dataquery.builder()
        .id(in.getId())
        .label(in.getLabel())
        .comment(in.getComment())
        .createdBy(in.getCreatedBy())
        .resultSize(in.getResultSize())
        .lastModified(in.getLastModified() == null ? null : in.getLastModified().toString())
        .content(jsonUtil.readValue(in.getCrtdl(), Crtdl.class))
        .expiresAt(in.getExpiresAt())
        .build();
  }
}
