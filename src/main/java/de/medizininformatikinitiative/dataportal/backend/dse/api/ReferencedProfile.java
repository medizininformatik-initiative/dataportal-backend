package de.medizininformatikinitiative.dataportal.backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record ReferencedProfile(
    @JsonProperty String url,
    @JsonProperty DisplayEntry display,
    @JsonProperty FieldDisplayEntry fields
) {
}
