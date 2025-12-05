package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import de.medizininformatikinitiative.dataportal.backend.common.api.TermCode;
import jakarta.persistence.Id;
import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.List;

@Builder
@Document(indexName = "codeable_concept")
public record CodeableConceptDocument(
    @Id String id,
    @Field(name = "termcode")
    TermCode termCode,
    @Field(name = "value_sets")
    List<String> valueSets,
    @Field(name = "display")
    Display display
) {
}
