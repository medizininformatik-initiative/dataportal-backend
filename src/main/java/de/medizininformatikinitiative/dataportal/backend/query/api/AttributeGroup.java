package de.medizininformatikinitiative.dataportal.backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.net.URI;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
public record AttributeGroup(
    @JsonProperty String id,
    @JsonProperty String name,
    @JsonProperty URI groupReference,
    @JsonProperty Boolean includeReferenceOnly,
    @JsonProperty List<Attribute> attributes,
    @JsonProperty List<Filter> filter
) {
  public AttributeGroup {
    attributes = attributes == null ? List.of() : attributes;
    filter = filter == null ? List.of() : filter;
  }
}
