package de.fdpg.dataportal_backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.fdpg.dataportal_backend.query.api.AttributeFilter;
import de.fdpg.dataportal_backend.query.api.TimeRestriction;
import de.fdpg.dataportal_backend.query.api.ValueFilter;
import de.fdpg.dataportal_backend.query.api.status.ValidationIssue;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@JsonInclude(Include.NON_NULL)
@Builder
@Data
public class MutableCriterion {
    @JsonProperty("context") TermCode context;
    @JsonProperty("termCodes") List<TermCode> termCodes;
    @JsonProperty("attributeFilters") List<AttributeFilter> attributeFilters;
    @JsonProperty("valueFilter") ValueFilter valueFilter;
    @JsonProperty("timeRestriction") TimeRestriction timeRestriction;
    @JsonProperty("issues") List<ValidationIssue> validationIssues;

    public static MutableCriterion createMutableCriterion (Criterion criterion) {
      return MutableCriterion.builder()
          .termCodes(criterion.termCodes())
          .context(criterion.context())
          .attributeFilters(criterion.attributeFilters())
          .valueFilter(criterion.valueFilter())
          .timeRestriction(criterion.timeRestriction())
          .validationIssues(criterion.validationIssues())
          .build();
    }
}
