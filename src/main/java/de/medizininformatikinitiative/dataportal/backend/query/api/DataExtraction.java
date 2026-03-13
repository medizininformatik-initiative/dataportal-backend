package de.medizininformatikinitiative.dataportal.backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.query.api.validation.DataExtractionValidation;
import lombok.Builder;

import java.util.List;

@JsonInclude(Include.NON_EMPTY)
@Builder(toBuilder = true)
@DataExtractionValidation
public record DataExtraction(
    @JsonProperty(required = true) List<AttributeGroup> attributeGroups
) {
  public DataExtraction {
    attributeGroups = attributeGroups == null ? List.of() : attributeGroups;
  }

  public static DataExtraction withReplacedDateFilter(DataExtraction extraction, AttributeGroup targetGroup, Filter replacement) {

    AttributeGroup updatedGroup = targetGroup.toBuilder()
        .filter(targetGroup.filter().stream()
            .map(f -> "date".equals(f.type()) ? replacement : f)
            .toList())
        .build();

    DataExtraction updatedExtraction = extraction.toBuilder()
        .attributeGroups(extraction.attributeGroups().stream()
            .map(g -> g.equals(targetGroup) ? updatedGroup : g)
            .toList())
        .build();

    return updatedExtraction;
  }

  public static DataExtraction withReplacedAttribute(DataExtraction extraction, AttributeGroup targetGroup, Attribute original, Attribute replacement) {

    AttributeGroup updatedGroup = targetGroup.toBuilder()
        .attributes(targetGroup.attributes().stream()
            .map(a -> a.attributeRef().equals(original.attributeRef()) ? replacement : a)
            .toList())
        .build();

    return extraction.toBuilder()
        .attributeGroups(extraction.attributeGroups().stream()
            .map(g -> g.id().equals(targetGroup.id()) ? updatedGroup : g)
            .toList())
        .build();
  }

  public static DataExtraction withRemovedAttribute(DataExtraction extraction, AttributeGroup targetGroup, Attribute toRemove) {

    AttributeGroup updatedGroup = targetGroup.toBuilder()
        .attributes(targetGroup.attributes().stream()
            .filter(a -> !a.equals(toRemove))
            .toList())
        .build();

    return extraction.toBuilder()
        .attributeGroups(extraction.attributeGroups().stream()
            .map(g -> g.equals(targetGroup) ? updatedGroup : g)
            .toList())
        .build();
  }
}
