package de.fdpg.dataportal_backend.terminology.es.model;

import lombok.Builder;

@Builder
public record TermFilterValue(
    String label,
    long count
) {

}
