package de.medizininformatikinitiative.dataportal.backend.dse.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DseProfileTest {
  @Test
  void testEquals_sameId_shouldBeEqual() {
    DseProfile profile1 = new DseProfile();
    profile1.setId(1L);

    DseProfile profile2 = new DseProfile();
    profile2.setId(1L);

    assertEquals(profile1, profile2);
    assertEquals(profile1.hashCode(), profile2.hashCode());
  }

  @Test
  void testEquals_differentId_shouldNotBeEqual() {
    DseProfile profile1 = new DseProfile();
    profile1.setId(1L);

    DseProfile profile2 = new DseProfile();
    profile2.setId(2L);

    assertNotEquals(profile1, profile2);
  }

  @Test
  void testEquals_nullId_shouldNotBeEqual() {
    DseProfile profile1 = new DseProfile(); // id is null
    DseProfile profile2 = new DseProfile(); // id is null

    assertNotEquals(profile1, profile2);
  }

  @Test
  void testEquals_self_shouldBeEqual() {
    DseProfile profile = new DseProfile();
    profile.setId(1L);

    assertEquals(profile, profile);
  }

  @Test
  void testEquals_null_shouldNotBeEqual() {
    DseProfile profile = new DseProfile();
    profile.setId(1L);

    assertNotEquals(null, profile);
  }

  @Test
  void testEquals_differentClass_shouldNotBeEqual() {
    DseProfile profile = new DseProfile();
    profile.setId(1L);

    Object other = new Object();

    assertNotEquals(profile, other);
  }
}
