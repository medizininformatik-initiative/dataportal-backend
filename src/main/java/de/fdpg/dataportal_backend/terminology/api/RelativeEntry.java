package de.fdpg.dataportal_backend.terminology.api;

import de.fdpg.dataportal_backend.common.api.DisplayEntry;
import de.fdpg.dataportal_backend.terminology.es.model.Relative;
import lombok.Builder;

@Builder
public record RelativeEntry(
    DisplayEntry display,
    boolean selectable,
    String termcode,
    String terminology,
    String contextualizedTermcodeHash
) {
  public static RelativeEntry of(Relative relative) {
    return RelativeEntry.builder()
        .display(DisplayEntry.of(relative.display()))
        .selectable(relative.selectable())
        .termcode(relative.termcode())
        .terminology(relative.terminology())
        .contextualizedTermcodeHash(relative.contextualizedTermcodeHash())
        .build();
  }
}
