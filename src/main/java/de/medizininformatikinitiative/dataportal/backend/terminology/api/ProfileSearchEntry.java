package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileCategory;
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
    DisplayEntry module,
    List<DisplayEntry> categories,
    int availability
) {
  public static ProfileSearchEntry of(ProfileDocument document) {
    return ProfileSearchEntry.builder()
        .id(document.id())
        .name(document.name())
        .display(DisplayEntry.of(document.display()))
        .selectable(document.selectable())
        .url(document.url())
        .module(document.module() == null ? null : DisplayEntry.of(document.module().display()))
        .categories(toCategoryDisplayEntries(document.categories()))
        .availability(document.availability() == null ? 0 : document.availability())
        .build();
  }

  private static List<DisplayEntry> toCategoryDisplayEntries(Collection<ProfileCategory> categories) {
    return categories == null ? List.of() : categories.stream().map(c -> DisplayEntry.of(c.display())).filter(Objects::nonNull).toList();
  }
}
