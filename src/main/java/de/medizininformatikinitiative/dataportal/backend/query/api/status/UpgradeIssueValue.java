package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record UpgradeIssueValue(
    @NonNull String message,
    @NonNull String code
) {
}
