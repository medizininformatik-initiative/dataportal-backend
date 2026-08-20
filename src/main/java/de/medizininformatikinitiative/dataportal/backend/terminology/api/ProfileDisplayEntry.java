package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileDisplay;
import lombok.Builder;

@Builder
public record ProfileDisplayEntry(
    DisplayEntry display,
    DisplayEntry description
) {
  public static ProfileDisplayEntry of(ProfileDisplay display) {
    var entry = DisplayEntry.of(display);
    return entry == null ? null : ProfileDisplayEntry.builder().display(entry).build();
  }

  public static ProfileDisplayEntry of(ProfileDisplay display, ProfileDisplay description) {
    var displayEntry = DisplayEntry.of(display);
    return displayEntry == null ? null : ProfileDisplayEntry.builder()
        .display(displayEntry)
        .description(DisplayEntry.of(description))
        .build();
  }
}
