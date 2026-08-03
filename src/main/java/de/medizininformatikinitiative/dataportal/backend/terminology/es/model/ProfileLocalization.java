package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Field;

@Builder
public record ProfileLocalization(
    @Field(name = "de-DE")
    String deDe,
    @Field(name = "en-US")
    String enUs
) {
}
