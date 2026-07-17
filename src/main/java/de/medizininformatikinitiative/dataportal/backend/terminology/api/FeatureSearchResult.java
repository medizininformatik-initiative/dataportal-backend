package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FeatureSearchResult {
  private long totalHits;
  private List<FeatureSearchEntry> results;
}
