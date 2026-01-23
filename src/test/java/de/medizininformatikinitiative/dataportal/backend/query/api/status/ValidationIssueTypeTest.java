package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@Tag("query")
@Tag("validation")
class ValidationIssueTypeTest {

  @ParameterizedTest
  @EnumSource(ValidationIssueType.class)
  void testValueOf_succeeds(ValidationIssueType validationIssueType) {
    var issueCode = validationIssueType.code();

    var issue = ValidationIssueType.valueOf(issueCode);

    assertEquals(issue, validationIssueType);
  }

  @Test
  void testValueOf_throwsOnUnknown() {
    assertThrows(IllegalArgumentException.class, () -> ValidationIssueType.valueOf(-1));
  }

  @ParameterizedTest
  @EnumSource(ValidationIssueType.class)
  void testResolve_succeeds(ValidationIssueType validationIssueType) {
    var issueCode = validationIssueType.code();

    var issue = ValidationIssueType.resolve(issueCode);

    assertEquals(issue, validationIssueType);
  }

  @Test
  void testResolve_nullOnUnknown() {
    var issue = ValidationIssueType.resolve(-1);

    assertNull(issue);
  }
}
