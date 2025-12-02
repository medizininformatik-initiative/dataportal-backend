package de.fdpg.dataportal_backend.common.api;

import de.fdpg.dataportal_backend.query.api.TimeRestriction;
import de.fdpg.dataportal_backend.query.api.ValueFilter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.fdpg.dataportal_backend.common.api.Comparator.GREATER_EQUAL;
import static de.fdpg.dataportal_backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MutableCriterionTest {

  @NotNull
  private static TermCode createTermCode() {
    return TermCode.builder()
        .code("LL2191-6")
        .system("http://loinc.org")
        .display("Geschlecht")
        .build();
  }

  @NotNull
  private static TimeRestriction createTimeRestriction() {
    return TimeRestriction.builder()
        .afterDate("2021-01-01")
        .beforeDate("2021-12-31")
        .build();
  }

  @NotNull
  private static ValueFilter createValueFilter() {
    return ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_EQUAL)
        .quantityUnit(createUnit())
        .value(50.0)
        .build();
  }

  @NotNull
  private static Unit createUnit() {
    return Unit.builder()
        .code("kg")
        .display("kilogram")
        .build();
  }

  @Test
  void createMutableCriterion() {
    var immutableCriterion = Criterion.builder()
        .attributeFilters(List.of())
        .context(createTermCode())
        .validationIssues(List.of())
        .termCodes(List.of(createTermCode()))
        .timeRestriction(createTimeRestriction())
        .valueFilter(createValueFilter())
        .build();
    var mutableCriterion = MutableCriterion.builder()
        .attributeFilters(List.of())
        .context(createTermCode())
        .validationIssues(List.of())
        .termCodes(List.of(createTermCode()))
        .timeRestriction(createTimeRestriction())
        .valueFilter(createValueFilter())
        .build();

    var createdMutableCriterion = MutableCriterion.createMutableCriterion(immutableCriterion);

    assertEquals(mutableCriterion, createdMutableCriterion);
  }
}
