package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import lombok.Builder;

import java.util.List;

@Builder
public record CodeableConceptBulkSearchResult(
    List<CodeableConceptEntry> found,
    List<String> notFound
) {
  public CodeableConceptBulkSearchResult {
    found = found == null ? List.of() : found;
    notFound = notFound == null ? List.of() : notFound;
  }
}
