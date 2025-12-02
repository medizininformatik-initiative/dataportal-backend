package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude()
@Builder
public record QueryQuota(
    @JsonProperty("softLimit") QueryQuotaEntry soft,
    @JsonProperty("hardLimit") QueryQuotaEntry hard
) {
}
