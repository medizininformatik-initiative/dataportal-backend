package de.numcodex.feasibility_gui_backend.terminology.api;

import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.model.OntologyItemRelationsDocument;
import de.numcodex.feasibility_gui_backend.terminology.es.model.Relative;
import lombok.Builder;

import java.util.Collection;
import java.util.stream.Collectors;

@Builder
public record RelationEntry(
    DisplayEntry display,
    boolean selectable,
    Collection<RelativeEntry> parents,
    Collection<RelativeEntry> children,
    Collection<RelativeEntry> relatedTerms
) {

  public static RelationEntry of(OntologyItemRelationsDocument document) {
    return RelationEntry.builder()
        .display(DisplayEntry.of(document.display()))
        .selectable(document.selectable())
        .parents(document.parents().stream().map(RelativeEntry::of).collect(Collectors.toList()))
        .children(document.children().stream().map(RelativeEntry::of).collect(Collectors.toList()))
        .relatedTerms(document.relatedTerms().stream().map(RelativeEntry::of).collect(Collectors.toList()))
        .build();
  }
}
