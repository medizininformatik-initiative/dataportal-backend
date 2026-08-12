package de.medizininformatikinitiative.dataportal.backend.terminology.es.model;

import jakarta.persistence.Id;
import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.Collection;

@Builder
@Document(indexName = "profile")
public record ProfileDocument(
    @Id String id,
    String name,
    ProfileDisplay display,
    Collection<ProfileDisplay> description,
    boolean selectable,
    String url,
    ProfileDisplay module,
    @Field(name = "resource_type")
    ProfileDisplay resourceType,
    Collection<ProfileDisplay> categories,
    Collection<ProfileField> fields,
    Collection<ProfileRelative> parents,
    Collection<ProfileRelative> children,
    Integer availability
) {
}
