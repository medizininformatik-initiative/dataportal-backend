package de.fdpg.dataportal_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.sql.Timestamp;

@JsonInclude(Include.NON_EMPTY)
@Builder
public record QueryListEntry(
    @JsonProperty long id,
    @JsonProperty String label,
    @JsonProperty String comment,
    @JsonProperty Timestamp createdAt,
    @JsonProperty Long totalNumberOfPatients,
    @JsonProperty Boolean isValid
) {

}
