package de.medizininformatikinitiative.dataportal.backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Crtdl(
    @JsonProperty String version,
    @JsonProperty String display,
    @Valid @JsonProperty Ccdl cohortDefinition,
    @Valid @JsonProperty DataExtraction dataExtraction
) {
}
