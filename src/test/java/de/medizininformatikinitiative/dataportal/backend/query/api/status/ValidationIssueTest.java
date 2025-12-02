package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@Tag("query")
@Tag("validation")
class ValidationIssueTest {

  @ParameterizedTest
  @EnumSource(ValidationIssue.class)
  void testValueOf_succeeds(ValidationIssue validationIssue) {
    var issueCode = validationIssue.code();

    var issue = ValidationIssue.valueOf(issueCode);

    assertEquals(issue, validationIssue);
  }

  @Test
  void testValueOf_throwsOnUnknown() {
    assertThrows(IllegalArgumentException.class, () -> ValidationIssue.valueOf(-1));
  }

  @ParameterizedTest
  @EnumSource(ValidationIssue.class)
  void testResolve_succeeds(ValidationIssue validationIssue) {
    var issueCode = validationIssue.code();

    var issue = ValidationIssue.resolve(issueCode);

    assertEquals(issue, validationIssue);
  }

  @Test
  void testResolve_nullOnUnknown() {
    var issue = ValidationIssue.resolve(-1);

    assertNull(issue);
  }
}
