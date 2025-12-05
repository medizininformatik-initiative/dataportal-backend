package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.common.api.TermCode;
import de.medizininformatikinitiative.dataportal.backend.dse.api.LocalizedValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CriteriaProfileDataTest {

  @Test
  void testEquals_sameTermCodes_shouldBeEqual() {
    TermCode tc1 = TermCode.builder()
        .code("code")
        .system("system")
        .display("display")
        .version("version")
        .build();

    CriteriaProfileData a = CriteriaProfileData.builder()
        .id("id1")
        .display(null)
        .context(null)
        .termCodes(List.of(tc1))
        .uiProfileId(null)
        .build();

    CriteriaProfileData b = CriteriaProfileData.builder()
        .id("id2")
        .display(DisplayEntry.builder()
            .original("original")
            .translations(List.of(LocalizedValue.builder()
                .language("de-DE")
                .value("value")
                .build()))
            .build())
        .context(TermCode.builder()
            .code("code")
            .system("system")
            .display("display")
            .version("version")
            .build())
        .termCodes(List.of(tc1))
        .uiProfileId("Patient")
        .build();

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void testEquals_differentTermCodes_shouldNotBeEqual() {
    TermCode tc1 = TermCode.builder()
        .code("code1")
        .system("system")
        .display("display")
        .version("version")
        .build();
    TermCode tc2 = TermCode.builder()
        .code("code2")
        .system("system")
        .display("display")
        .version("version")
        .build();

    CriteriaProfileData a = CriteriaProfileData.builder()
        .termCodes(List.of(tc1))
        .build();

    CriteriaProfileData b = CriteriaProfileData.builder()
        .termCodes(List.of(tc2))
        .build();

    assertNotEquals(a, b);
  }

  @Test
  void testEquals_nullTermCodes_shouldEqualEmptyList() {
    CriteriaProfileData a = CriteriaProfileData.builder()
        .termCodes(null)
        .build();

    CriteriaProfileData b = CriteriaProfileData.builder()
        .termCodes(List.of())
        .build();

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void testEquals_null_shouldNotBeEqual() {
    CriteriaProfileData a = CriteriaProfileData.builder()
        .termCodes(List.of())
        .build();

    assertNotEquals(a, null);
  }

  @Test
  void testEquals_self_shouldBeEqual() {
    CriteriaProfileData a = CriteriaProfileData.builder()
        .termCodes(List.of())
        .build();

    assertEquals(a, a);
  }

  @Test
  void testEquals_differentClass_shouldNotBeEqual() {
    CriteriaProfileData a = CriteriaProfileData.builder()
        .termCodes(List.of())
        .build();

    assertNotEquals(a, new Object());
  }

  @Test
  void testAddDisplay() {
    TermCode tc = TermCode.builder()
        .code("code")
        .system("system")
        .display("display")
        .version("version")
        .build();

    CriteriaProfileData cpd = CriteriaProfileData.builder()
        .id("id")
        .context(TermCode.builder()
            .code("code")
            .system("system")
            .display("display")
            .version("version")
            .build())
        .termCodes(List.of(tc))
        .uiProfileId("Patient")
        .build();

    assertNull(cpd.display());
    CriteriaProfileData cpdWithDisplay = cpd.addDisplay(DisplayEntry.builder().build());
    assertNotNull(cpdWithDisplay.display());
  }

}
