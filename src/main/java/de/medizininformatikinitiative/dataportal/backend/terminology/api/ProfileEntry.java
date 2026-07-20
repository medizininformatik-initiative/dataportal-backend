package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileCategory;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileDocument;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileField;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileRelative;
import lombok.Builder;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Builder
public record ProfileEntry(
    String id,
    String name,
    DisplayEntry display,
    DisplayEntry description,
    boolean selectable,
    String url,
    DisplayEntry module,
    List<DisplayEntry> categories,
    int availability,
    List<DisplayEntry> fields,
    List<ProfileRelativeEntry> parents,
    List<ProfileRelativeEntry> children
) {
  public static ProfileEntry of(ProfileDocument document) {
    return ProfileEntry.builder()
        .id(document.id())
        .name(document.name())
        .display(DisplayEntry.of(document.display()))
        .description(DisplayEntry.of(document.description()))
        .selectable(document.selectable())
        .url(document.url())
        .module(document.module() == null ? null : DisplayEntry.of(document.module().display()))
        .categories(toCategoryDisplayEntries(document.categories()))
        .availability(document.availability() == null ? 0 : document.availability())
        .fields(toDisplayEntries(document.fields()))
        .parents(toRelativeEntries(document.parents()))
        .children(toRelativeEntries(document.children()))
        .build();
  }

  private static List<DisplayEntry> toDisplayEntries(Collection<ProfileField> fields) {
    return fields == null ? List.of() : fields.stream().map(f -> DisplayEntry.of(f.display())).filter(Objects::nonNull).toList();
  }

  private static List<DisplayEntry> toCategoryDisplayEntries(Collection<ProfileCategory> categories) {
    return categories == null ? List.of() : categories.stream().map(c -> DisplayEntry.of(c.display())).filter(Objects::nonNull).toList();
  }

  private static List<ProfileRelativeEntry> toRelativeEntries(Collection<ProfileRelative> relatives) {
    return relatives == null ? List.of() : relatives.stream().map(ProfileRelativeEntry::of).toList();
  }
}
