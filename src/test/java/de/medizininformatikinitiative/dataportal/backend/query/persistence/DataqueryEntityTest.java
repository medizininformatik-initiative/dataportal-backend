package de.medizininformatikinitiative.dataportal.backend.query.persistence;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DataqueryEntityTest {

  private DataqueryEntity createDataquery(Long id, String label) {
    var dataquery = new DataqueryEntity();
    dataquery.setId(id);
    dataquery.setCreatedBy("some-user");
    dataquery.setLabel(label);
    dataquery.setComment("some comment");
    dataquery.setCrtdl("{}");
    dataquery.setLastModified(Timestamp.valueOf("2024-01-01 00:00:00"));
    dataquery.setResultSize(42L);
    dataquery.setExpiresAt(Timestamp.valueOf("2025-01-01 00:00:00"));
    return dataquery;
  }

  @Test
  void testEqualsAndHashCode_sameObject() {
    var dataquery = createDataquery(1L, "some label");

    assertEquals(dataquery, dataquery);
    assertEquals(dataquery.hashCode(), dataquery.hashCode());
  }

  @Test
  void testEqualsAndHashCode_equalFields() {
    var dataquery1 = createDataquery(1L, "some label");
    var dataquery2 = createDataquery(1L, "some label");

    assertEquals(dataquery1, dataquery2);
    assertEquals(dataquery2, dataquery1);
    assertEquals(dataquery1.hashCode(), dataquery2.hashCode());
  }

  @Test
  void testEqualsAndHashCode_differentFields() {
    var dataquery1 = createDataquery(1L, "some label");
    var dataquery2 = createDataquery(2L, "another label");

    assertNotEquals(dataquery1, dataquery2);
    assertNotEquals(dataquery2, dataquery1);
  }

  @Test
  void testEquals_nullObjectNotEquals() {
    var dataquery = createDataquery(1L, "some label");

    assertNotEquals(dataquery, null);
    assertNotEquals(null, dataquery);
  }

  @Test
  void testEquals_otherClassNotEquals() {
    var dataquery = createDataquery(1L, "some label");
    Integer i = 10;

    assertNotEquals(dataquery, i);
  }
}
