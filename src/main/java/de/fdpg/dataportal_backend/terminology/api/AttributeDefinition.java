package de.fdpg.dataportal_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.fdpg.dataportal_backend.common.api.Comparator;
import de.fdpg.dataportal_backend.common.api.DisplayEntry;
import de.fdpg.dataportal_backend.common.api.TermCode;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record AttributeDefinition(
    @JsonProperty(value = "display", required = true) DisplayEntry display,
    @JsonProperty(value = "type", required = true) ValueDefinitonType type,
    @JsonProperty("selectableConcepts") List<TermCode> selectableConcepts,
    @JsonProperty("attributeCode") TermCode attributeCode,
    @JsonProperty("comparator") Comparator comparator,
    @JsonProperty("optional") Boolean optional,
    @JsonProperty("allowedUnits") List<TermCode> allowedUnits,
    @JsonProperty(value = "precision", required = true, defaultValue = "0") double precision,
    @JsonProperty(value = "min") Double min,
    @JsonProperty(value = "max") Double max,
    @JsonProperty("referencedCriteriaSet") List<String> referencedCriteriaSets,
    @JsonProperty("referencedValueSet") List<String> referencedValueSets
) {
  public AttributeDefinition {
    selectableConcepts = (selectableConcepts == null) ? List.of() : selectableConcepts;
    allowedUnits = (allowedUnits == null) ? List.of() : allowedUnits;
    referencedCriteriaSets = (referencedCriteriaSets == null) ? List.of() : referencedCriteriaSets;
    referencedValueSets = (referencedValueSets == null) ? List.of() : referencedValueSets;
  }
}