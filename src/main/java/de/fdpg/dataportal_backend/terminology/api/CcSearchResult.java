package de.fdpg.dataportal_backend.terminology.api;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CcSearchResult {
  private long totalHits;
  private List<CodeableConceptEntry> results;
}
