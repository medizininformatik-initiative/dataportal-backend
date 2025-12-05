package de.medizininformatikinitiative.dataportal.backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record LocalizedValueList(
    @JsonProperty String language,
    @JsonProperty List<String> value
) {
  public LocalizedValueList {
    value = (value == null) ? List.of() : value;
  }
}
