package de.medizininformatikinitiative.dataportal.backend.terminology.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TermCodeEntityTest {
  @Test
  void testEquals_sameId_shouldBeEqual() {
    TermCodeEntity termCode1 = new TermCodeEntity();
    termCode1.setId(1L);

    TermCodeEntity termCode2 = new TermCodeEntity();
    termCode2.setId(1L);

    assertEquals(termCode1, termCode2);
    assertEquals(termCode1.hashCode(), termCode2.hashCode());
  }

  @Test
  void testEquals_differentId_shouldNotBeEqual() {
    TermCodeEntity termCode1 = new TermCodeEntity();
    termCode1.setId(1L);

    TermCodeEntity termCode2 = new TermCodeEntity();
    termCode2.setId(2L);

    assertNotEquals(termCode1, termCode2);
  }

  @Test
  void testEquals_nullId_shouldNotBeEqual() {
    TermCodeEntity termCode1 = new TermCodeEntity(); // id is null
    TermCodeEntity termCode2 = new TermCodeEntity(); // id is null

    assertNotEquals(termCode1, termCode2);
  }

  @Test
  void testEquals_self_shouldBeEqual() {
    TermCodeEntity termCode = new TermCodeEntity();
    termCode.setId(1L);

    assertEquals(termCode, termCode);
  }

  @Test
  void testEquals_null_shouldNotBeEqual() {
    TermCodeEntity termCode = new TermCodeEntity();
    termCode.setId(1L);

    assertNotEquals(null, termCode);
  }

  @Test
  void testEquals_differentClass_shouldNotBeEqual() {
    TermCodeEntity termCode = new TermCodeEntity();
    termCode.setId(1L);

    Object other = new Object();

    assertNotEquals(termCode, other);
  }
}
