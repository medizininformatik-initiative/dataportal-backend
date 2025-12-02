package de.fdpg.dataportal_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record UiProfile(
    @JsonProperty("name") String name,
    @JsonProperty("timeRestrictionAllowed") boolean timeRestrictionAllowed,
    @JsonProperty("valueDefinition") AttributeDefinition valueDefinition,
    @JsonProperty("attributeDefinitions") List<AttributeDefinition> attributeDefinitions
) {
  public UiProfile {
    attributeDefinitions = (attributeDefinitions == null) ? List.of() : attributeDefinitions;
  }
}
