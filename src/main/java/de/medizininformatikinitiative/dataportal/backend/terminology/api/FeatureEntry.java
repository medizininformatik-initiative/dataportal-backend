package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.FeatureDocument;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.FeatureRelative;
import lombok.Builder;

import java.util.Collection;
import java.util.List;

@Builder
public record FeatureEntry(
    String id,
    String name,
    DisplayEntry display,
    DisplayEntry description,
    boolean selectable,
    String url,
    DisplayEntry module,
    List<String> categories,
    int availability,
    List<FeatureRelativeEntry> parents,
    List<FeatureRelativeEntry> children
) {
  public static FeatureEntry of(FeatureDocument document) {
    return FeatureEntry.builder()
        .id(document.id())
        .name(document.name())
        .display(DisplayEntry.of(document.display()))
        .description(document.description() == null ? null : DisplayEntry.of(document.description()))
        .selectable(document.selectable())
        .url(document.url())
        .module(document.module() == null ? null : DisplayEntry.of(document.module().display()))
        .categories(document.categories() == null ? List.of() : List.copyOf(document.categories()))
        .availability(document.availability() == null ? 0 : document.availability())
        .parents(toRelativeEntries(document.parents()))
        .children(toRelativeEntries(document.children()))
        .build();
  }

  private static List<FeatureRelativeEntry> toRelativeEntries(Collection<FeatureRelative> relatives) {
    return relatives == null ? List.of() : relatives.stream().map(FeatureRelativeEntry::of).toList();
  }
}
