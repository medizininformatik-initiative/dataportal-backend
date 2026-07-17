package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.FeatureDocument;
import lombok.Builder;

import java.util.List;

@Builder
public record FeatureSearchEntry(
    String id,
    String name,
    DisplayEntry display,
    boolean selectable,
    String url,
    DisplayEntry module,
    List<String> categories,
    int availability
) {
  public static FeatureSearchEntry of(FeatureDocument document) {
    return FeatureSearchEntry.builder()
        .id(document.id())
        .name(document.name())
        .display(DisplayEntry.of(document.display()))
        .selectable(document.selectable())
        .url(document.url())
        .module(document.module() == null ? null : DisplayEntry.of(document.module().display()))
        .categories(document.categories() == null ? List.of() : List.copyOf(document.categories()))
        .availability(document.availability() == null ? 0 : document.availability())
        .build();
  }
}
