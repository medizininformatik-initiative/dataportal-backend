package de.medizininformatikinitiative.dataportal.backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.common.api.Comparator;
import de.medizininformatikinitiative.dataportal.backend.common.api.TermCode;
import de.medizininformatikinitiative.dataportal.backend.common.api.Unit;
import lombok.Builder;

import java.util.List;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
@Builder
public record ValueFilter(
    @JsonProperty(value = "type", required = true) ValueFilterType type,
    @JsonProperty("selectedConcepts") List<TermCode> selectedConcepts,
    @JsonProperty("comparator") Comparator comparator,
    @JsonProperty("unit") Unit quantityUnit,
    @JsonProperty(value = "value") Double value,
    @JsonProperty(value = "minValue") Double minValue,
    @JsonProperty(value = "maxValue") Double maxValue
) {
  public ValueFilter {
    Objects.requireNonNull(type);
    selectedConcepts = selectedConcepts == null ? List.of() : selectedConcepts;
  }
}
