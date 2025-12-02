package de.fdpg.dataportal_backend.terminology.api;

import de.fdpg.dataportal_backend.common.api.DisplayEntry;
import de.fdpg.dataportal_backend.common.api.TermCode;
import de.fdpg.dataportal_backend.terminology.es.model.OntologyItemDocument;
import lombok.Builder;

import java.util.List;

@Builder
public record EsSearchResultEntryExtended(
    String id,
    DisplayEntry display,
    int availability,
    TermCode context,
    String terminology,
    List<TermCode> termcodes,
    String kdsModule,
    boolean selectable
) {
  public static EsSearchResultEntryExtended of(OntologyItemDocument ontologyItemDocument) {
    return EsSearchResultEntryExtended.builder()
        .id(ontologyItemDocument.id())
        .display(DisplayEntry.of(ontologyItemDocument.display()))
        .availability(ontologyItemDocument.availability())
        .context(ontologyItemDocument.context())
        .terminology(ontologyItemDocument.terminology())
        .termcodes(ontologyItemDocument.termCodes() == null ? List.of() : ontologyItemDocument.termCodes().stream().toList())
        .kdsModule(ontologyItemDocument.kdsModule())
        .selectable(ontologyItemDocument.selectable())
        .build();
  }
}
