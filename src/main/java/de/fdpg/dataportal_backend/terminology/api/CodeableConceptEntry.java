package de.fdpg.dataportal_backend.terminology.api;

import de.fdpg.dataportal_backend.common.api.DisplayEntry;
import de.fdpg.dataportal_backend.common.api.TermCode;
import de.fdpg.dataportal_backend.dse.api.LocalizedValue;
import de.fdpg.dataportal_backend.terminology.es.model.CodeableConceptDocument;
import lombok.Builder;

import java.util.List;

@Builder
public record CodeableConceptEntry(
    String id,
    TermCode termCode,
    DisplayEntry display
) {
  public static CodeableConceptEntry of(CodeableConceptDocument document) {
    return CodeableConceptEntry.builder()
        .id(document.id())
        .termCode(document.termCode())
        .display(DisplayEntry.builder()
            .original(document.display().original())
            .translations(List.of(
                LocalizedValue.builder()
                    .language("de-DE")
                    .value(document.display().deDe())
                    .build(),
                LocalizedValue.builder()
                    .language("en-US")
                    .value(document.display().enUs())
                    .build()
            ))
            .build()
        )
        .build();
  }
}
