package de.fdpg.dataportal_backend.query.api.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fdpg.dataportal_backend.common.api.Criterion;
import de.fdpg.dataportal_backend.common.api.TermCode;
import de.fdpg.dataportal_backend.common.api.Unit;
import de.fdpg.dataportal_backend.query.api.StructuredQuery;
import de.fdpg.dataportal_backend.query.api.TimeRestriction;
import de.fdpg.dataportal_backend.query.api.ValueFilter;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static de.fdpg.dataportal_backend.common.api.Comparator.GREATER_EQUAL;
import static de.fdpg.dataportal_backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("query")
@Tag("api")
@Tag("validation")
@ExtendWith(MockitoExtension.class)
public class StructuredQueryValidatorTest {
  public static StructuredQueryValidator validator;

  @Mock
  private ConstraintValidatorContext constraintValidatorContext;

  @BeforeAll
  public static void setUp() throws IOException {
    validator = new StructuredQueryValidator(new ObjectMapper());
  }

  @Test
  public void testValidate_validQueryOk() {
    var structuredQuery = buildValidQuery();
    assertTrue(validator.isValid(structuredQuery, constraintValidatorContext));
  }

  @Disabled("For this intermediate step, the validator always returns valid. This will change again...so just disable this for now")
  @Test
  public void testValidate_invalidQueriesFail() {
    var queryWithoutVersion = buildInvalidQueryWithoutVersion();
    assertFalse(validator.isValid(queryWithoutVersion, constraintValidatorContext));

    var queryWithoutInclusionCriteria = buildInvalidQueryWithoutInclusionCriteria();
    assertFalse(validator.isValid(queryWithoutInclusionCriteria, constraintValidatorContext));

    var queryWithEmptyInclusionCriteria = buildInvalidQueryWithEmptyInclusionCriteria();
    assertFalse(validator.isValid(queryWithEmptyInclusionCriteria, constraintValidatorContext));

    var queryWithEmptyCriterionTermCodes = buildInvalidQueryWithEmptyTermCodes();
    assertFalse(validator.isValid(queryWithEmptyCriterionTermCodes, constraintValidatorContext));

    var queryWithEmptyTermCodeCodes = buildInvalidQueryWithEmptyTermCodeCodes();
    assertFalse(validator.isValid(queryWithEmptyTermCodeCodes, constraintValidatorContext));

    var queryWithEmptyTermCodeSystems = buildInvalidQueryWithEmptyTermCodeSystems();
    assertFalse(validator.isValid(queryWithEmptyTermCodeSystems, constraintValidatorContext));

    var queryWithEmptyTermCodeDisplays = buildInvalidQueryWithEmptyTermCodeDisplays();
    assertFalse(validator.isValid(queryWithEmptyTermCodeDisplays, constraintValidatorContext));

    var queryWithMalformedTimeRestrictions = buildInvalidQueryWithMalformedTimeRestrictions();
    assertFalse(validator.isValid(queryWithMalformedTimeRestrictions, constraintValidatorContext));

    var queryWithTimeRestrictionsWithoutDates = buildInvalidQueryWithTimeRestrictionsWithoutDates();
    assertFalse(
        validator.isValid(queryWithTimeRestrictionsWithoutDates, constraintValidatorContext));
  }

  private StructuredQuery buildValidQuery() {
    var bodyWeightTermCode = TermCode.builder()
        .code("27113001")
        .system("http://snomed.info/sct")
        .version("v1")
        .display("Body weight (observable entity)")
        .build();
    var contextTermCode = TermCode.builder()
        .code("Diagnose")
        .system("fdpg.mii.cds")
        .version("1.0.0")
        .display("Diagnose")
        .build();
    var kgUnit = Unit.builder()
        .code("kg")
        .display("kilogram")
        .build();
    var bodyWeightValueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_EQUAL)
        .quantityUnit(kgUnit)
        .value(50.0)
        .build();
    var timeRestriction = TimeRestriction.builder()
        .afterDate("2021-01-01")
        .beforeDate("2021-12-31")
        .build();
    var hasBmiGreaterThanFifty = Criterion.builder()
        .termCodes(List.of(bodyWeightTermCode))
        .context(contextTermCode)
        .valueFilter(bodyWeightValueFilter)
        .timeRestriction(timeRestriction)
        .build();
    return StructuredQuery.builder()
        .display("display-value")
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .exclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .build();
  }

  private StructuredQuery buildInvalidQueryWithoutVersion() {
    var validQuery = buildValidQuery();
    return StructuredQuery.builder()
        .display("display-value")
        .inclusionCriteria(validQuery.inclusionCriteria())
        .exclusionCriteria(validQuery.exclusionCriteria())
        .display(validQuery.display())
        .build();
  }

  private StructuredQuery buildInvalidQueryWithoutInclusionCriteria() {
    var validQuery = buildValidQuery();
    return StructuredQuery.builder()
        .display("display-value")
        .version(validQuery.version())
        .exclusionCriteria(validQuery.exclusionCriteria())
        .display(validQuery.display())
        .build();
  }

  private StructuredQuery buildInvalidQueryWithEmptyInclusionCriteria() {
    var validQuery = buildValidQuery();
    return StructuredQuery.builder()
        .display("display-value")
        .version(validQuery.version())
        .inclusionCriteria(List.of())
        .exclusionCriteria(validQuery.exclusionCriteria())
        .display(validQuery.display())
        .build();
  }

  private StructuredQuery buildInvalidQueryWithEmptyTermCodes() {
    var bodyWeightTermCode = TermCode.builder().build();
    var kgUnit = Unit.builder()
        .code("kg")
        .display("kilogram")
        .build();
    var bodyWeightValueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_EQUAL)
        .quantityUnit(kgUnit)
        .value(50.0)
        .build();
    var timeRestriction = TimeRestriction.builder()
        .afterDate("2021-01-01")
        .beforeDate("2021-12-31")
        .build();
    var hasBmiGreaterThanFifty = Criterion.builder()
        .termCodes(List.of(bodyWeightTermCode))
        .valueFilter(bodyWeightValueFilter)
        .timeRestriction(timeRestriction)
        .build();
    return StructuredQuery.builder()
        .display("display-value")
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .exclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .build();
  }

  private StructuredQuery buildInvalidQueryWithEmptyTermCodeCodes() {
    var bodyWeightTermCode = TermCode.builder()
        .system("http://snomed.info/sct")
        .version("v1")
        .display("Body weight (observable entity)")
        .build();
    var kgUnit = Unit.builder()
        .code("kg")
        .display("kilogram")
        .build();
    var bodyWeightValueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_EQUAL)
        .quantityUnit(kgUnit)
        .value(50.0)
        .build();
    var timeRestriction = TimeRestriction.builder()
        .afterDate("2021-01-01")
        .beforeDate("2021-12-31")
        .build();
    var hasBmiGreaterThanFifty = Criterion.builder()
        .termCodes(List.of(bodyWeightTermCode))
        .valueFilter(bodyWeightValueFilter)
        .timeRestriction(timeRestriction)
        .build();
    return StructuredQuery.builder()
        .display("display-value")
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .exclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .build();
  }

  private StructuredQuery buildInvalidQueryWithEmptyTermCodeSystems() {
    var bodyWeightTermCode = TermCode.builder()
        .code("27113001")
        .version("v1")
        .display("Body weight (observable entity)")
        .build();
    var kgUnit = Unit.builder()
        .code("kg")
        .display("kilogram")
        .build();
    var bodyWeightValueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_EQUAL)
        .quantityUnit(kgUnit)
        .value(50.0)
        .build();
    var timeRestriction = TimeRestriction.builder()
        .afterDate("2021-01-01")
        .beforeDate("2021-12-31")
        .build();
    var hasBmiGreaterThanFifty = Criterion.builder()
        .termCodes(List.of(bodyWeightTermCode))
        .valueFilter(bodyWeightValueFilter)
        .timeRestriction(timeRestriction)
        .build();
    return StructuredQuery.builder()
        .display("display-value")
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .exclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .build();
  }

  private StructuredQuery buildInvalidQueryWithEmptyTermCodeDisplays() {
    var bodyWeightTermCode = TermCode.builder()
        .code("27113001")
        .system("http://snomed.info/sct")
        .version("v1")
        .build();
    var kgUnit = Unit.builder()
        .code("kg")
        .display("kilogram")
        .build();
    var bodyWeightValueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_EQUAL)
        .quantityUnit(kgUnit)
        .value(50.0)
        .build();
    var timeRestriction = TimeRestriction.builder()
        .afterDate("2021-01-01")
        .beforeDate("2021-12-31")
        .build();
    var hasBmiGreaterThanFifty = Criterion.builder()
        .termCodes(List.of(bodyWeightTermCode))
        .valueFilter(bodyWeightValueFilter)
        .timeRestriction(timeRestriction)
        .build();
    return StructuredQuery.builder()
        .display("display-value")
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .exclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .build();
  }

  private StructuredQuery buildInvalidQueryWithMalformedTimeRestrictions() {
    var bodyWeightTermCode = TermCode.builder()
        .code("27113001")
        .system("http://snomed.info/sct")
        .version("v1")
        .display("Body weight (observable entity)")
        .build();
    var kgUnit = Unit.builder()
        .code("kg")
        .display("kilogram")
        .build();
    var bodyWeightValueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_EQUAL)
        .quantityUnit(kgUnit)
        .value(50.0)
        .build();
    var timeRestriction = TimeRestriction.builder()
        .afterDate("foo")
        .beforeDate("bar")
        .build();
    var hasBmiGreaterThanFifty = Criterion.builder()
        .termCodes(List.of(bodyWeightTermCode))
        .valueFilter(bodyWeightValueFilter)
        .timeRestriction(timeRestriction)
        .build();
    return StructuredQuery.builder()
        .display("display-value")
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .exclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .build();
  }

  private StructuredQuery buildInvalidQueryWithTimeRestrictionsWithoutDates() {
    var bodyWeightTermCode = TermCode.builder()
        .code("27113001")
        .system("http://snomed.info/sct")
        .version("v1")
        .display("Body weight (observable entity)")
        .build();
    var kgUnit = Unit.builder()
        .code("kg")
        .display("kilogram")
        .build();
    var bodyWeightValueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_EQUAL)
        .quantityUnit(kgUnit)
        .value(50.0)
        .build();
    var timeRestriction = TimeRestriction.builder().build();
    var hasBmiGreaterThanFifty = Criterion.builder()
        .termCodes(List.of(bodyWeightTermCode))
        .valueFilter(bodyWeightValueFilter)
        .timeRestriction(timeRestriction)
        .build();
    return StructuredQuery.builder()
        .display("display-value")
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .exclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .build();
  }

}
