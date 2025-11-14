package de.numcodex.feasibility_gui_backend.terminology.es.model;

import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.Collection;

@Builder
@Document(indexName = "ontology")
public record OntologyItemRelationsDocument(
    @Field(name = "display")
    Display display,
    @Field(name = "selectable")
    boolean selectable,
    String terminology,
    @Field(name = "term_code")
    String termcode,
    @Field(name = "parents")
    Collection<Relative> parents,
    @Field(name = "children")
    Collection<Relative> children,
    @Field(name = "related_terms")
    Collection<Relative> relatedTerms
) {
}
