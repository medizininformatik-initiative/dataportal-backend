package de.fdpg.dataportal_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Crtdl(
    @JsonProperty String version,
    @JsonProperty String display,
    @JsonProperty StructuredQuery cohortDefinition,
    @JsonProperty DataExtraction dataExtraction
) {
}
