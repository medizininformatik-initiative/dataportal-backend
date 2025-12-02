package de.fdpg.dataportal_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.fdpg.dataportal_backend.common.api.Comparator;
import de.fdpg.dataportal_backend.common.api.Criterion;
import de.fdpg.dataportal_backend.common.api.TermCode;
import de.fdpg.dataportal_backend.common.api.Unit;
import lombok.Builder;

import java.util.List;
import java.util.Objects;


@JsonInclude(Include.NON_EMPTY)
@Builder
public record AttributeFilter(
    @JsonProperty List<Criterion> criteria,
    @JsonProperty(value = "type", required = true) ValueFilterType type,
    @JsonProperty("selectedConcepts") List<TermCode> selectedConcepts,
    @JsonProperty("comparator") Comparator comparator,
    @JsonProperty("unit") Unit quantityUnit,
    @JsonProperty(value = "value") Double value,
    @JsonProperty(value = "minValue") Double minValue,
    @JsonProperty(value = "maxValue") Double maxValue,
    @JsonProperty(value = "attributeCode", required = true) TermCode attributeCode
) {

    public AttributeFilter {
        Objects.requireNonNull(type);
        Objects.requireNonNull(attributeCode);
        criteria = criteria == null ? List.of() : criteria;
        selectedConcepts = selectedConcepts == null ? List.of() : selectedConcepts;
    }
}
