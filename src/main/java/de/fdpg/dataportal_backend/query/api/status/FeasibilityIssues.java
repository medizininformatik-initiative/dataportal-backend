package de.fdpg.dataportal_backend.query.api.status;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@JsonSerialize()
@Builder
public record FeasibilityIssues(
        List<FeasibilityIssue> issues
) {
  public FeasibilityIssues {
    issues = issues == null ? List.of() : issues;
  }
}
