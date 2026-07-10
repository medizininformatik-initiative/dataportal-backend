package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.List;

@Builder
public record FeatureFields(
    List<String> original,
    @Field(name = "de")
    List<String> deDe,
    @Field(name = "en")
    List<String> enUs
) {
}
