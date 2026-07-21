package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileDisplay;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileDocument;
import lombok.Builder;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Builder
public record ProfileSearchEntry(
    String id,
    String name,
    DisplayEntry display,
    boolean selectable,
    String url,
    ProfileDisplayEntry module,
    List<ProfileDisplayEntry> categories,
    int availability
) {
  public static ProfileSearchEntry of(ProfileDocument document) {
    return ProfileSearchEntry.builder()
        .id(document.id())
        .name(document.name())
        .display(DisplayEntry.of(document.display()))
        .selectable(document.selectable())
        .url(document.url())
        .module(ProfileDisplayEntry.of(document.module()))
        .categories(toCategoryDisplayEntries(document.categories()))
        .availability(document.availability() == null ? 0 : document.availability())
        .build();
  }

  private static List<ProfileDisplayEntry> toCategoryDisplayEntries(Collection<ProfileDisplay> categories) {
    return categories == null ? List.of() : categories.stream().map(ProfileDisplayEntry::of).filter(Objects::nonNull).toList();
  }
}
