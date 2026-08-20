package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@Tag("query")
@Tag("validation")
class UpgradeIssueTypeTest {

  @ParameterizedTest
  @EnumSource(UpgradeIssueType.class)
  void testValueOf_succeeds(UpgradeIssueType upgradeIssueType) {
    var issueCode = upgradeIssueType.code();

    var issue = UpgradeIssueType.valueOf(issueCode);

    assertEquals(issue, upgradeIssueType);
  }

  @Test
  void testValueOf_throwsOnUnknown() {
    assertThrows(IllegalArgumentException.class, () -> UpgradeIssueType.valueOf(-1));
  }

  @ParameterizedTest
  @EnumSource(UpgradeIssueType.class)
  void testResolve_succeeds(UpgradeIssueType upgradeIssueType) {
    var issueCode = upgradeIssueType.code();

    var issue = UpgradeIssueType.resolve(issueCode);

    assertEquals(issue, upgradeIssueType);
  }

  @Test
  void testResolve_nullOnUnknown() {
    var issue = UpgradeIssueType.resolve(-1);

    assertNull(issue);
  }
}
