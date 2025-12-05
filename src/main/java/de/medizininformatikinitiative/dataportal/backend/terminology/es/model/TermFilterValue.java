package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import lombok.Builder;

@Builder
public record TermFilterValue(
    String label,
    long count
) {

}
