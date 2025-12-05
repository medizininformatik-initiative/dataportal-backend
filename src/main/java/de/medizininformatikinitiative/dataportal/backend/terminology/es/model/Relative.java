package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Field;

@Builder
public record Relative(
    Display display,
    boolean selectable,
    String name,
    String terminology,
    @Field(name = "term_code")
    String termcode,
    @Field(name = "contextualized_termcode_hash")
    String contextualizedTermcodeHash
) {
}
