package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record TerminologySystemEntry(
    @JsonProperty String url,
    @JsonProperty String name,
    @JsonProperty DisplayEntry display
) {
}
