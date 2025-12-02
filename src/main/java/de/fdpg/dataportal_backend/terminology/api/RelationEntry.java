package de.fdpg.dataportal_backend.terminology.api;

import de.fdpg.dataportal_backend.common.api.DisplayEntry;
import de.fdpg.dataportal_backend.terminology.es.model.OntologyItemRelationsDocument;
import lombok.Builder;

import java.util.Collection;
import java.util.stream.Collectors;

@Builder
public record RelationEntry(
    DisplayEntry display,
    boolean selectable,
    String termcode,
    String terminology,
    Collection<RelativeEntry> parents,
    Collection<RelativeEntry> children,
    Collection<RelativeEntry> relatedTerms
) {

  public static RelationEntry of(OntologyItemRelationsDocument document) {
    return RelationEntry.builder()
        .display(DisplayEntry.of(document.display()))
        .selectable(document.selectable())
        .termcode(document.termcode())
        .terminology(document.terminology())
        .parents(document.parents().stream().map(RelativeEntry::of).collect(Collectors.toList()))
        .children(document.children().stream().map(RelativeEntry::of).collect(Collectors.toList()))
        .relatedTerms(document.relatedTerms().stream().map(RelativeEntry::of).collect(Collectors.toList()))
        .build();
  }
}
