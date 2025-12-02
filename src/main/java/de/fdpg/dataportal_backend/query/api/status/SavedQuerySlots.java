package de.fdpg.dataportal_backend.query.api.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude()
@Builder
public record SavedQuerySlots(
    @JsonProperty("used") long used,
    @JsonProperty("total") long total
) {
}
