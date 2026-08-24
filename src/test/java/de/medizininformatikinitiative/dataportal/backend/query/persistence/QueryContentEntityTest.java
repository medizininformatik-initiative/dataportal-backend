package de.medizininformatikinitiative.dataportal.backend.query.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class QueryContentEntityTest {
  @Test
  void testEquals_sameId_shouldBeEqual() {
    QueryContentEntity content1 = new QueryContentEntity();
    content1.setId(1L);

    QueryContentEntity content2 = new QueryContentEntity();
    content2.setId(1L);

    assertEquals(content1, content2);
    assertEquals(content1.hashCode(), content2.hashCode());
  }

  @Test
  void testEquals_differentId_shouldNotBeEqual() {
    QueryContentEntity content1 = new QueryContentEntity();
    content1.setId(1L);

    QueryContentEntity content2 = new QueryContentEntity();
    content2.setId(2L);

    assertNotEquals(content1, content2);
  }

  @Test
  void testEquals_nullId_shouldNotBeEqual() {
    QueryContentEntity content1 = new QueryContentEntity(); // id is null
    QueryContentEntity content2 = new QueryContentEntity(); // id is null

    assertNotEquals(content1, content2);
  }

  @Test
  void testEquals_self_shouldBeEqual() {
    QueryContentEntity content = new QueryContentEntity();
    content.setId(1L);

    assertEquals(content, content);
  }

  @Test
  void testEquals_null_shouldNotBeEqual() {
    QueryContentEntity content = new QueryContentEntity();
    content.setId(1L);

    assertNotEquals(null, content);
  }

  @Test
  void testEquals_differentClass_shouldNotBeEqual() {
    QueryContentEntity content = new QueryContentEntity();
    content.setId(1L);

    Object other = new Object();

    assertNotEquals(content, other);
  }
}
