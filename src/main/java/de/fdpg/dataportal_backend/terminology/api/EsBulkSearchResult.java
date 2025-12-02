package de.fdpg.dataportal_backend.terminology.api;

import lombok.Builder;

import java.util.List;

@Builder
public record EsBulkSearchResult(
    String uiProfileId,
    List<EsSearchResultEntryExtended> found,
    List<String> notFound
) {
  public EsBulkSearchResult {
    found = found == null ? List.of() : found;
    notFound = notFound == null ? List.of() : notFound;
  }

  public EsBulkSearchResult withUiProfileId(String uiProfileId) {
    return new EsBulkSearchResult(uiProfileId, found, notFound);
  }
}
