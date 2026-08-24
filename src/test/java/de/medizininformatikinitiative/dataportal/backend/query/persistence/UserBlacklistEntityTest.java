package de.medizininformatikinitiative.dataportal.backend.query.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UserBlacklistEntityTest {
  @Test
  void testEquals_sameId_shouldBeEqual() {
    UserBlacklistEntity entry1 = new UserBlacklistEntity();
    entry1.setId(1L);

    UserBlacklistEntity entry2 = new UserBlacklistEntity();
    entry2.setId(1L);

    assertEquals(entry1, entry2);
    assertEquals(entry1.hashCode(), entry2.hashCode());
  }

  @Test
  void testEquals_differentId_shouldNotBeEqual() {
    UserBlacklistEntity entry1 = new UserBlacklistEntity();
    entry1.setId(1L);

    UserBlacklistEntity entry2 = new UserBlacklistEntity();
    entry2.setId(2L);

    assertNotEquals(entry1, entry2);
  }

  @Test
  void testEquals_nullId_shouldNotBeEqual() {
    UserBlacklistEntity entry1 = new UserBlacklistEntity(); // id is null
    UserBlacklistEntity entry2 = new UserBlacklistEntity(); // id is null

    assertNotEquals(entry1, entry2);
  }

  @Test
  void testEquals_self_shouldBeEqual() {
    UserBlacklistEntity entry = new UserBlacklistEntity();
    entry.setId(1L);

    assertEquals(entry, entry);
  }

  @Test
  void testEquals_null_shouldNotBeEqual() {
    UserBlacklistEntity entry = new UserBlacklistEntity();
    entry.setId(1L);

    assertNotEquals(null, entry);
  }

  @Test
  void testEquals_differentClass_shouldNotBeEqual() {
    UserBlacklistEntity entry = new UserBlacklistEntity();
    entry.setId(1L);

    Object other = new Object();

    assertNotEquals(entry, other);
  }
}
