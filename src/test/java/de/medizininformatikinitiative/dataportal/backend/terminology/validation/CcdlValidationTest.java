package de.medizininformatikinitiative.dataportal.backend.terminology.validation;

import de.medizininformatikinitiative.dataportal.backend.common.api.Criterion;
import de.medizininformatikinitiative.dataportal.backend.common.api.TermCode;
import de.medizininformatikinitiative.dataportal.backend.query.api.Ccdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.TimeRestriction;
import de.medizininformatikinitiative.dataportal.backend.terminology.TerminologyService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;

@Tag("terminology")
@ExtendWith(MockitoExtension.class)
class CcdlValidationTest {

  @Mock
  private TerminologyService terminologyService;

  private CcdlValidation ccdlValidation;

  @NotNull
  private static Ccdl createValidCcdl(boolean withExclusionCriteria) {
    var context = TermCode.builder()
        .code("Laboruntersuchung")
        .system("fdpg.mii.cds")
        .display("Laboruntersuchung")
        .version("1.0.0")
        .build();
    var termCode = TermCode.builder()
        .code("19113-0")
        .system("http://loinc.org")
        .display("IgE")
        .build();
    var criterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .context(context)
        .attributeFilters(List.of())
        .build();
    return Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(criterion)))
        .exclusionCriteria(withExclusionCriteria ? List.of(List.of(criterion)) : List.of())
        .display("foo")
        .build();
  }

  @NotNull
  private static Ccdl createCcdlWithoutContext() {
    var termCode = TermCode.builder()
        .code("19113-0")
        .system("http://loinc.org")
        .display("IgE")
        .build();
    var criterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .attributeFilters(List.of())
        .build();
    return Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(criterion)))
        .exclusionCriteria(List.of())
        .display("foo")
        .build();
  }

  @NotNull
  private static Ccdl createCcdlWithInvalidTimeRestriction() {
    var context = TermCode.builder()
        .code("Laboruntersuchung")
        .system("fdpg.mii.cds")
        .display("Laboruntersuchung")
        .version("1.0.0")
        .build();
    var termCode = TermCode.builder()
        .code("19113-0")
        .system("http://loinc.org")
        .display("IgE")
        .build();
    var timeRestriction = TimeRestriction.builder()
        .afterDate("1998-05-09")
        .beforeDate("1991-06-15")
        .build();
    var criterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .context(context)
        .attributeFilters(List.of())
        .timeRestriction(timeRestriction)
        .build();
    return Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(criterion)))
        .exclusionCriteria(List.of())
        .display("foo")
        .build();
  }

  @NotNull
  private static Ccdl createCcdlWithOnlyBeforeDate() {
    var context = TermCode.builder()
        .code("Laboruntersuchung")
        .system("fdpg.mii.cds")
        .display("Laboruntersuchung")
        .version("1.0.0")
        .build();
    var termCode = TermCode.builder()
        .code("19113-0")
        .system("http://loinc.org")
        .display("IgE")
        .build();
    var timeRestriction = TimeRestriction.builder()
        .beforeDate("1991-06-15")
        .build();
    var criterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .context(context)
        .attributeFilters(List.of())
        .timeRestriction(timeRestriction)
        .build();
    return Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(criterion)))
        .exclusionCriteria(List.of())
        .display("foo")
        .build();
  }

  @NotNull
  private static Ccdl createCcdlWithOnlyAfterDate() {
    var context = TermCode.builder()
        .code("Laboruntersuchung")
        .system("fdpg.mii.cds")
        .display("Laboruntersuchung")
        .version("1.0.0")
        .build();
    var termCode = TermCode.builder()
        .code("19113-0")
        .system("http://loinc.org")
        .display("IgE")
        .build();
    var timeRestriction = TimeRestriction.builder()
        .afterDate("1998-05-09")
        .build();
    var criterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .context(context)
        .attributeFilters(List.of())
        .timeRestriction(timeRestriction)
        .build();
    return Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(criterion)))
        .exclusionCriteria(List.of())
        .display("foo")
        .build();
  }

  @BeforeEach
  void setUp() {
    ccdlValidation = new CcdlValidation(terminologyService);
  }

  @Test
  void testInvalidOnNull() {
    var isValid = ccdlValidation.isValid(null);

    assertFalse(isValid);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testIsValid_trueOnValidCriteria(boolean withExclusionCriteria) {
    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));

    var isValid = ccdlValidation.isValid(createValidCcdl(withExclusionCriteria));

    assertTrue(isValid);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testIsValid_falseOnInvalidCriteria(boolean withExclusionCriteria) {
    doReturn(false).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));

    var isValid = ccdlValidation.isValid(createValidCcdl(withExclusionCriteria));

    assertFalse(isValid);
  }

  @Test
  void testIsValid_falseOnMissingContext() {
    var isValid = ccdlValidation.isValid(createCcdlWithoutContext());

    assertFalse(isValid);
  }

  @Test
  void testIsValid_falseOnInvalidTimeRestriction() {
    var isValid = ccdlValidation.isValid(createCcdlWithInvalidTimeRestriction());

    assertFalse(isValid);
  }

  @Test
  void testIsValid_trueOnOnlyBeforeDateTimeRestriction() {
    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    var isValid = ccdlValidation.isValid(createCcdlWithOnlyBeforeDate());

    assertTrue(isValid);
  }

  @Test
  void testIsValid_trueOnOnlyAfterDateTimeRestriction() {
    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    var isValid = ccdlValidation.isValid(createCcdlWithOnlyAfterDate());

    assertTrue(isValid);
  }

  @ParameterizedTest
  @CsvSource({"true,true", "true,false", "false,true", "false,false"})
  void testAnnotateCcdl_emptyIssuesOnValidCriteriaOrSkippedValidation(String withExclusionCriteriaString, String skipValidationString) {
    boolean withExclusionCriteria = Boolean.parseBoolean(withExclusionCriteriaString);
    boolean skipValidation = Boolean.parseBoolean(skipValidationString);
    if (!skipValidation) {
      doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    }

    var annotatedCcdl = ccdlValidation.annotateCcdl(createValidCcdl(withExclusionCriteria), skipValidation);

    assertTrue(annotatedCcdl.inclusionCriteria().get(0).get(0).validationIssues().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"true,true", "true,false", "false,true", "false,false"})
  void testAnnotateCcdl_nonEmptyIssuesOnInvalidCriteria(String withExclusionCriteriaString, String skipValidationString) {
    boolean withExclusionCriteria = Boolean.parseBoolean(withExclusionCriteriaString);
    boolean skipValidation = Boolean.parseBoolean(skipValidationString);
    if (!skipValidation) {
      doReturn(false).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    }

    var annotatedCcdl = ccdlValidation.annotateCcdl(createValidCcdl(withExclusionCriteria), skipValidation);

    if (skipValidation) {
      assertTrue(annotatedCcdl.inclusionCriteria().get(0).get(0).validationIssues().isEmpty());
    } else {
      assertFalse(annotatedCcdl.inclusionCriteria().get(0).get(0).validationIssues().isEmpty());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testAnnotateCcdl_nonEmptyIssuesOnMissingContext(boolean skipValidation) {
    var annotatedCcdl = ccdlValidation.annotateCcdl(createCcdlWithoutContext(), skipValidation);

    if (skipValidation) {
      assertTrue(annotatedCcdl.inclusionCriteria().get(0).get(0).validationIssues().isEmpty());
    } else {
      assertFalse(annotatedCcdl.inclusionCriteria().get(0).get(0).validationIssues().isEmpty());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testAnnotateCcdl_nonEmptyIssuesOnInvalidTimeRestriction(boolean skipValidation) {
    var annotatedCcdl = ccdlValidation.annotateCcdl(createCcdlWithInvalidTimeRestriction(), skipValidation);

    if (skipValidation) {
      assertTrue(annotatedCcdl.inclusionCriteria().get(0).get(0).validationIssues().isEmpty());
    } else {
      assertFalse(annotatedCcdl.inclusionCriteria().get(0).get(0).validationIssues().isEmpty());
    }
  }
}
