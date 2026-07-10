package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Field;

@Builder
public record FeatureRelative(
    @Field(name = "hashed-url")
    String hashedUrl,
    Display display,
    String terminology,
    @Field(name = "term_code")
    String termCode,
    boolean selectable
) {
}
