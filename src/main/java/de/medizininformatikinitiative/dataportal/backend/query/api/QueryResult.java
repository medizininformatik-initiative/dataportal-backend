package de.medizininformatikinitiative.dataportal.backend.query.api;

import lombok.Builder;

import java.util.List;

@Builder
public record QueryResult(
    long totalNumberOfPatients,
    Long queryId,
    List<QueryResultLine> resultLines
) {
  public QueryResult {
    resultLines = resultLines == null ? List.of() : resultLines;
  }
}
