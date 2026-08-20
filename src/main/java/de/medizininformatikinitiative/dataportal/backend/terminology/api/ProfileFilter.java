package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import lombok.Builder;

import java.util.List;

@Builder
public record ProfileFilter(
    String name,
    List<ProfileFilterValue> values
) {
}
