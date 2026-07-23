package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TermFilter(
    String name,
    String type,
    List<TermFilterValue> values
) {
}
