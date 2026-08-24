package de.medizininformatikinitiative.dataportal.backend.dse.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DseProfileEntityTest {
  @Test
  void testEquals_sameId_shouldBeEqual() {
    DseProfileEntity profile1 = new DseProfileEntity();
    profile1.setId(1L);

    DseProfileEntity profile2 = new DseProfileEntity();
    profile2.setId(1L);

    assertEquals(profile1, profile2);
    assertEquals(profile1.hashCode(), profile2.hashCode());
  }

  @Test
  void testEquals_differentId_shouldNotBeEqual() {
    DseProfileEntity profile1 = new DseProfileEntity();
    profile1.setId(1L);

    DseProfileEntity profile2 = new DseProfileEntity();
    profile2.setId(2L);

    assertNotEquals(profile1, profile2);
  }

  @Test
  void testEquals_nullId_shouldNotBeEqual() {
    DseProfileEntity profile1 = new DseProfileEntity(); // id is null
    DseProfileEntity profile2 = new DseProfileEntity(); // id is null

    assertNotEquals(profile1, profile2);
  }

  @Test
  void testEquals_self_shouldBeEqual() {
    DseProfileEntity profile = new DseProfileEntity();
    profile.setId(1L);

    assertEquals(profile, profile);
  }

  @Test
  void testEquals_null_shouldNotBeEqual() {
    DseProfileEntity profile = new DseProfileEntity();
    profile.setId(1L);

    assertNotEquals(null, profile);
  }

  @Test
  void testEquals_differentClass_shouldNotBeEqual() {
    DseProfileEntity profile = new DseProfileEntity();
    profile.setId(1L);

    Object other = new Object();

    assertNotEquals(profile, other);
  }
}
