package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;

import java.util.List;

@Builder
public record TerminologyBulkSearchRequest(
    @JsonProperty("terminology") @NonNull String terminology,
    @JsonProperty("context") @NonNull String context,
    @JsonProperty("searchterms") @NonNull List<String> searchterms
) {
}
