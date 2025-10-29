package de.numcodex.feasibility_gui_backend.query.api;

import lombok.Builder;

import java.util.List;

@Builder
public record QueryResult(
    long resultSize,
    Long queryId,
    List<QueryResultLine> resultLines
) {
  public QueryResult {
    resultLines = resultLines == null ? List.of() : resultLines;
  }
}
