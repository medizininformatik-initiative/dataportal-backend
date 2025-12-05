package de.medizininformatikinitiative.dataportal.backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record LocalizedValue(
    @JsonProperty String language,
    @JsonProperty String value
) {
}
