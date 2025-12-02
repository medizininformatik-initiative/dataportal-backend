package de.fdpg.dataportal_backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.fdpg.dataportal_backend.common.api.DisplayEntry;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record ReferencedProfile(
    @JsonProperty String url,
    @JsonProperty DisplayEntry display,
    @JsonProperty FieldDisplayEntry fields
) {
}
