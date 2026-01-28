package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record ValidationIssueValue(
    @NonNull String message,
    @NonNull String code
) {
}
