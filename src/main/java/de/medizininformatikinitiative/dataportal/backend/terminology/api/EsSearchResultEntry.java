package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.OntologyListItemDocument;
import lombok.Builder;

@Builder
public record EsSearchResultEntry(
    String id,
    DisplayEntry display,
    int availability,
    String context,
    String terminology,
    String termcode,
    String kdsModule,
    boolean selectable
) {
  public static EsSearchResultEntry of(OntologyListItemDocument ontologyListItemDocument) {
    return EsSearchResultEntry.builder()
        .id(ontologyListItemDocument.id())
        .display(DisplayEntry.of(ontologyListItemDocument.display()))
        .availability(ontologyListItemDocument.availability())
        .context(ontologyListItemDocument.context().code())
        .terminology(ontologyListItemDocument.terminology())
        .termcode(ontologyListItemDocument.termcode())
        .kdsModule(ontologyListItemDocument.kdsModule())
        .selectable(ontologyListItemDocument.selectable())
        .build();
  }
}
