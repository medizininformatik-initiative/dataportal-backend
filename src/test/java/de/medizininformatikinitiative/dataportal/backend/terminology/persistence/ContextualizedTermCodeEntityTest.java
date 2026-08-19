package de.medizininformatikinitiative.dataportal.backend.terminology.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ContextualizedTermCodeEntityTest {

  private ContextualizedTermCodeEntity createEntry(String hash) {
    var entry = new ContextualizedTermCodeEntity();
    entry.setContextTermcodeHash(hash);
    entry.setContextId(1);
    entry.setTermCodeId(1);
    entry.setUiProfileId(1);
    return entry;
  }

  @Test
  void testEquals_sameHash_shouldBeEqual() {
    var entry1 = createEntry("hash1");
    var entry2 = createEntry("hash1");

    assertEquals(entry1, entry2);
    assertEquals(entry1.hashCode(), entry2.hashCode());
  }

  @Test
  void testEquals_differentHash_shouldNotBeEqual() {
    var entry1 = createEntry("hash1");
    var entry2 = createEntry("hash2");

    assertNotEquals(entry1, entry2);
  }

  @Test
  void testEquals_nullHash_shouldNotBeEqual() {
    var entry1 = createEntry(null);
    var entry2 = createEntry(null);

    assertNotEquals(entry1, entry2);
  }

  @Test
  void testEquals_self_shouldBeEqual() {
    var entry = createEntry("hash1");

    assertEquals(entry, entry);
  }

  @Test
  void testEquals_null_shouldNotBeEqual() {
    var entry = createEntry("hash1");

    assertNotEquals(null, entry);
  }

  @Test
  void testEquals_differentClass_shouldNotBeEqual() {
    var entry = createEntry("hash1");
    Object other = new Object();

    assertNotEquals(entry, other);
  }
}
