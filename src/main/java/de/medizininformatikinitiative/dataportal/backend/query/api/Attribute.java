package de.medizininformatikinitiative.dataportal.backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
public record Attribute(
    @JsonProperty String attributeRef,
    @JsonProperty boolean mustHave,
    @JsonProperty List<String> linkedGroups
) {
  public Attribute {
    linkedGroups = linkedGroups == null ? List.of() : linkedGroups;
  }
}
