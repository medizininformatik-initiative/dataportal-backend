package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import lombok.Builder;

@Builder
public record ProfileDisplay(
    String original,
    ProfileLocalization localization
) {
}
