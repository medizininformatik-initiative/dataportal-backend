package de.medizininformatikinitiative.dataportal.backend.validation.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.query.api.Crtdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.UpgradeIssue;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record UpgradeWrapper(
    @JsonProperty Crtdl crtdl,
    @JsonProperty List<UpgradeIssue> annotations
    ) {
}
