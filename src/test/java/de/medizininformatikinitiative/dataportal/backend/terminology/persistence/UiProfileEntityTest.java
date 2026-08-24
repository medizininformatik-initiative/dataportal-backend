package de.medizininformatikinitiative.dataportal.backend.terminology.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UiProfileEntityTest {
  @Test
  void testEquals_sameId_shouldBeEqual() {
    UiProfileEntity profile1 = new UiProfileEntity();
    profile1.setId(1L);

    UiProfileEntity profile2 = new UiProfileEntity();
    profile2.setId(1L);

    assertEquals(profile1, profile2);
    assertEquals(profile1.hashCode(), profile2.hashCode());
  }

  @Test
  void testEquals_differentId_shouldNotBeEqual() {
    UiProfileEntity profile1 = new UiProfileEntity();
    profile1.setId(1L);

    UiProfileEntity profile2 = new UiProfileEntity();
    profile2.setId(2L);

    assertNotEquals(profile1, profile2);
  }

  @Test
  void testEquals_nullId_shouldNotBeEqual() {
    UiProfileEntity profile1 = new UiProfileEntity(); // id is null
    UiProfileEntity profile2 = new UiProfileEntity(); // id is null

    assertNotEquals(profile1, profile2);
  }

  @Test
  void testEquals_self_shouldBeEqual() {
    UiProfileEntity profile = new UiProfileEntity();
    profile.setId(1L);

    assertEquals(profile, profile);
  }

  @Test
  void testEquals_null_shouldNotBeEqual() {
    UiProfileEntity profile = new UiProfileEntity();
    profile.setId(1L);

    assertNotEquals(null, profile);
  }

  @Test
  void testEquals_differentClass_shouldNotBeEqual() {
    UiProfileEntity profile = new UiProfileEntity();
    profile.setId(1L);

    Object other = new Object();

    assertNotEquals(profile, other);
  }
}
