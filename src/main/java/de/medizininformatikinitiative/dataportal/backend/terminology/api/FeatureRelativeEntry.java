package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.FeatureRelative;
import lombok.Builder;

@Builder
public record FeatureRelativeEntry(
    String id,
    DisplayEntry display,
    String url
) {
  public static FeatureRelativeEntry of(FeatureRelative relative) {
    return FeatureRelativeEntry.builder()
        .id(relative.id())
        .display(DisplayEntry.of(relative.display()))
        .url(relative.url())
        .build();
  }
}
