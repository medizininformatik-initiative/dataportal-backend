package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import jakarta.persistence.Id;
import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.Collection;

@Builder
@Document(indexName = "feature")
public record FeatureDocument(
    @Id String id,
    String name,
    Display display,
    Display description,
    boolean selectable,
    String module,
    Collection<String> categories,
    FeatureFields fields,
    Collection<FeatureRelative> parents,
    Collection<FeatureRelative> children,
    int availability
) {
}
