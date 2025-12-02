package de.fdpg.dataportal_backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.fdpg.dataportal_backend.query.api.AttributeFilter;
import de.fdpg.dataportal_backend.query.api.TimeRestriction;
import de.fdpg.dataportal_backend.query.api.ValueFilter;
import de.fdpg.dataportal_backend.query.api.status.ValidationIssue;
import lombok.Builder;

import java.util.List;

@JsonInclude(Include.NON_NULL)
@Builder
public record Criterion (
    @JsonProperty("context") TermCode context,
    @JsonProperty("termCodes") List<TermCode> termCodes,
    @JsonProperty("attributeFilters") List<AttributeFilter> attributeFilters,
    @JsonProperty("valueFilter") ValueFilter valueFilter,
    @JsonProperty("timeRestriction") TimeRestriction timeRestriction,
    @JsonProperty("issues") List<ValidationIssue> validationIssues
) {
  public static Criterion createImmutableCriterion(MutableCriterion mutableCriterion) {
    return Criterion.builder()
        .termCodes(mutableCriterion.termCodes)
        .context(mutableCriterion.context)
        .attributeFilters(mutableCriterion.attributeFilters)
        .valueFilter(mutableCriterion.valueFilter)
        .timeRestriction(mutableCriterion.timeRestriction)
        .validationIssues(mutableCriterion.validationIssues)
        .build();
  }
}
