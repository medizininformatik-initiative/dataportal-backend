package de.fdpg.dataportal_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude(Include.NON_NULL)
@Builder
public record TimeRestriction(
    @JsonProperty("beforeDate") String beforeDate,
    @JsonProperty("afterDate") String afterDate
) {

}
