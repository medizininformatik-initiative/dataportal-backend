package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileRelative;
import lombok.Builder;

@Builder
public record ProfileRelativeEntry(
    String id,
    DisplayEntry display,
    String url,
    boolean selectable
) {
  public static ProfileRelativeEntry of(ProfileRelative relative) {
    return ProfileRelativeEntry.builder()
        .id(relative.id())
        .display(DisplayEntry.of(relative.display()))
        .url(relative.url())
        .selectable(relative.selectable())
        .build();
  }
}
