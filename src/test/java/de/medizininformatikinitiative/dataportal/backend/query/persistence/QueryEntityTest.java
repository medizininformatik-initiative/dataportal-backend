package de.medizininformatikinitiative.dataportal.backend.query.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class QueryEntityTest {
  @Test
  void testEquals_sameId_shouldBeEqual() {
    QueryEntity query1 = new QueryEntity();
    query1.setId(1L);

    QueryEntity query2 = new QueryEntity();
    query2.setId(1L);

    assertEquals(query1, query2);
    assertEquals(query1.hashCode(), query2.hashCode());
  }

  @Test
  void testEquals_differentId_shouldNotBeEqual() {
    QueryEntity query1 = new QueryEntity();
    query1.setId(1L);

    QueryEntity query2 = new QueryEntity();
    query2.setId(2L);

    assertNotEquals(query1, query2);
  }

  @Test
  void testEquals_nullId_shouldNotBeEqual() {
    QueryEntity query1 = new QueryEntity(); // id is null
    QueryEntity query2 = new QueryEntity(); // id is null

    assertNotEquals(query1, query2);
  }

  @Test
  void testEquals_self_shouldBeEqual() {
    QueryEntity query = new QueryEntity();
    query.setId(1L);

    assertEquals(query, query);
  }

  @Test
  void testEquals_null_shouldNotBeEqual() {
    QueryEntity query = new QueryEntity();
    query.setId(1L);

    assertNotEquals(null, query);
  }

  @Test
  void testEquals_differentClass_shouldNotBeEqual() {
    QueryEntity query = new QueryEntity();
    query.setId(1L);

    Object other = new Object();

    assertNotEquals(query, other);
  }
}
