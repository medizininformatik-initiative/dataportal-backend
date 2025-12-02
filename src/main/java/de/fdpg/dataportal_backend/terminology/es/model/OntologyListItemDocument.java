package de.fdpg.dataportal_backend.terminology.es.model;

import de.fdpg.dataportal_backend.common.api.TermCode;
import jakarta.persistence.Id;
import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

@Builder
@Document(indexName = "ontology")
public record OntologyListItemDocument(
    @Id String id,
    Display display,
    int availability,
    TermCode context,
    String terminology,
    String termcode,
    @Field(name = "kds_module")
    String kdsModule,
    boolean selectable
) {
}
