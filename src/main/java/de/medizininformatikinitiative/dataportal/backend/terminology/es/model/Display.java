package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Field;

@Builder
public record Display(
    String original,
    @Field(name = "de")
    String deDe,
    @Field(name = "en")
    String enUs
) {
}
