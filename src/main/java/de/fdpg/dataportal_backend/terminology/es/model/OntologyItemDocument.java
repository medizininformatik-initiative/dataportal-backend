package de.fdpg.dataportal_backend.terminology.es.model;

import de.fdpg.dataportal_backend.common.api.TermCode;
import jakarta.persistence.Id;
import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.Collection;

@Builder
@Document(indexName = "ontology")
public record OntologyItemDocument(
    @Id String id,
    int availability,
    boolean selectable,
    TermCode context,
    @Field(name = "termcodes")
    Collection<TermCode> termCodes,
    String terminology,
    String termcode,
    @Field(name = "display")
    Display display,
    @Field(name = "kds_module")
    String kdsModule,
    @Field(name = "criteria_sets")
    Collection<String> criteriaSets,
    @Field(name = "parents")
    Collection<Relative> parents,
    @Field(name = "children")
    Collection<Relative> children,
    @Field(name = "related_terms")
    Collection<Relative> relatedTerms
) {
}
