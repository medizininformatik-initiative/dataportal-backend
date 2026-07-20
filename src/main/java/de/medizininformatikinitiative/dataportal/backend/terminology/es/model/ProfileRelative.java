package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import lombok.Builder;

@Builder
public record ProfileRelative(
    String id,
    ProfileDisplay display,
    String url
) {
}
