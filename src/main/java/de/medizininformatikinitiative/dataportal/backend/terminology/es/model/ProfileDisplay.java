package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.Map;

@Builder
public record ProfileDisplay(
    String original,
    @Field(name = "localization")
    Map<String, String> translations
) {
}
