package de.fdpg.dataportal_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
public record CrtdlSectionInfo(
    @JsonProperty boolean exists,
    @JsonProperty boolean isValid
) {
}
