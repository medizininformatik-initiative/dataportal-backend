package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import lombok.Builder;
import lombok.NonNull;

import java.util.Map;

@Builder
public record UpgradeIssue(
    @NonNull String path,
    @NonNull UpgradeIssueValue value,
    Map<String, Object> details
) {
  public UpgradeIssue {
    details = details == null ? Map.of() : details;
  }
}
