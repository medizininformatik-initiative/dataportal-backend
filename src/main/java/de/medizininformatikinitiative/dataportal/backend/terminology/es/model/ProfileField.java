package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import lombok.Builder;

@Builder
public record ProfileField(
    ProfileDisplay display,
    ProfileDisplay description
) {
}
