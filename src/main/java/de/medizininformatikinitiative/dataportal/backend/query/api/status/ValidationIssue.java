package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import lombok.Builder;
import lombok.NonNull;

import java.util.Map;

@Builder
public record ValidationIssue(
    @NonNull String path,
    @NonNull ValidationIssueValue value,
    Map<String, Object> details
) {
  public ValidationIssue {
    details = details == null ? Map.of() : details;
  }
}
