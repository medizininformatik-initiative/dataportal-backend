package de.medizininformatikinitiative.dataportal.backend.terminology.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CodingTest {

  @Test
  void testEquals_sameFields_shouldBeEqual() {
    Coding c1 = new Coding("http://loinc.org", "1234-5", "1.0");
    Coding c2 = new Coding("http://loinc.org", "1234-5", "1.0");

    assertEquals(c1, c2);
    assertEquals(c1.hashCode(), c2.hashCode());
  }

  @Test
  void testEquals_differentSystem_shouldNotBeEqual() {
    Coding c1 = new Coding("http://loinc.org", "1234-5", "1.0");
    Coding c2 = new Coding("http://snomed.info", "1234-5", "1.0");

    assertNotEquals(c1, c2);
  }

  @Test
  void testEquals_differentCode_shouldNotBeEqual() {
    Coding c1 = new Coding("http://loinc.org", "1234-5", "1.0");
    Coding c2 = new Coding("http://loinc.org", "6789-0", "1.0");

    assertNotEquals(c1, c2);
  }

  @Test
  void testEquals_differentVersion_shouldNotBeEqual() {
    Coding c1 = new Coding("http://loinc.org", "1234-5", "1.0");
    Coding c2 = new Coding("http://loinc.org", "1234-5", "2.0");

    assertNotEquals(c1, c2);
  }

  @Test
  void testEquals_null_shouldNotBeEqual() {
    Coding c1 = new Coding("http://loinc.org", "1234-5", "1.0");

    assertNotEquals(c1, null);
  }

  @Test
  void testEquals_differentClass_shouldNotBeEqual() {
    Coding c1 = new Coding("http://loinc.org", "1234-5", "1.0");
    String other = "not a Coding";

    assertNotEquals(c1, other);
  }

  @Test
  void testEquals_self_shouldBeEqual() {
    Coding c1 = new Coding("http://loinc.org", "1234-5", "1.0");

    assertEquals(c1, c1);
  }
}
