package de.medizininformatikinitiative.dataportal.backend.query.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryResultRateLimit {
  private long limit;
  private long remaining;
}
