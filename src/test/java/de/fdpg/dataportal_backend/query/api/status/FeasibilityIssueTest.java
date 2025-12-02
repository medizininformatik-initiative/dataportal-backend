package de.fdpg.dataportal_backend.query.api.status;


import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;


@Tag("query")
@Tag("validation")
class FeasibilityIssueTest {

  @ParameterizedTest
  @EnumSource(FeasibilityIssue.class)
  void testValueOf_succeeds(FeasibilityIssue feasibilityIssue) {
    var issueCode = feasibilityIssue.code();

    var issue = FeasibilityIssue.valueOf(issueCode);

    assertEquals(issue, feasibilityIssue);
  }

  @Test
  void testValueOf_throwsOnUnknown() {
    assertThrows(IllegalArgumentException.class, () -> FeasibilityIssue.valueOf(-1));
  }

  @ParameterizedTest
  @EnumSource(FeasibilityIssue.class)
  void testResolve_succeeds(FeasibilityIssue feasibilityIssue) {
    var issueCode = feasibilityIssue.code();

    var issue = FeasibilityIssue.resolve(issueCode);

    assertEquals(issue, feasibilityIssue);
  }

  @Test
  void testResolve_nullOnUnknown() {
    var issue = FeasibilityIssue.resolve(-1);

    assertNull(issue);
  }
}
