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
    String url,
    FeatureModule module,
    Collection<String> categories,
    Collection<FeatureField> fields,
    Collection<FeatureRelative> parents,
    Collection<FeatureRelative> children,
    Integer availability
) {
}
