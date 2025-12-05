package de.medizininformatikinitiative.dataportal.backend.terminology.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UiProfileContentTest {

  @Test
  void testEqualsAndHashCode_sameUiProfileContent() {
    var uiProfileContent = createUiProfileContent("some uiprofile", "some system", "some code", "some version");

    assertEquals(uiProfileContent, uiProfileContent);
    assertEquals(uiProfileContent.hashCode(), uiProfileContent.hashCode());
  }

  @Test
  void testEqualsAndHashCode_equalUiProfileContent() {
    var uiProfileContent1 = createUiProfileContent("some uiprofile", "some system", "some code", "some version");
    var uiProfileContent2 = createUiProfileContent("some uiprofile", "some system", "some code", "some version");

    assertEquals(uiProfileContent1, uiProfileContent2);
    assertEquals(uiProfileContent2, uiProfileContent1);
    assertEquals(uiProfileContent1.hashCode(), uiProfileContent2.hashCode());
  }

  @Test
  void testEqualsAndHashCode_differentUiProfileContent() {
    var uiProfileContent1 = createUiProfileContent("some uiprofile", "some system", "some code", "some version");
    // the actual ui profile is not part of the equals method
    var uiProfileContent2 = createUiProfileContent("some uiprofile", "another system", "another code", "another version");

    assertNotEquals(uiProfileContent1, uiProfileContent2);
    assertNotEquals(uiProfileContent2, uiProfileContent1);
    assertNotEquals(uiProfileContent1.hashCode(), uiProfileContent2.hashCode());
  }

  @Test
  void testEquals_nullObjectNotEquals() {
    var uiProfileContent = createUiProfileContent("some uiprofile", "some system", "some code", "some version");

    assertNotEquals(uiProfileContent, null);
  }

  @Test
  void testEqualsAndHashCode_otherClassNotEquals() {
    var uiProfileContent = createUiProfileContent("some uiprofile", "some system", "some code", "some version");
    Integer i = 10;

    assertNotEquals(uiProfileContent, i);
    assertNotEquals(uiProfileContent.hashCode(), i.hashCode());
  }

  private UiProfileContent createUiProfileContent(String uiProfile, String system, String code, String version) {
    UiProfileContent uiProfileContent = new UiProfileContent();
    uiProfileContent.setUiProfile(uiProfile);
    uiProfileContent.setSystem(system);
    uiProfileContent.setCode(code);
    uiProfileContent.setVersion(version);
    return uiProfileContent;
  }
}
