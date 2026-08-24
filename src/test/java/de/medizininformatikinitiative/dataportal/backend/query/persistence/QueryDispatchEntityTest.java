package de.medizininformatikinitiative.dataportal.backend.query.persistence;

import de.medizininformatikinitiative.dataportal.backend.query.persistence.QueryDispatchEntity.QueryDispatchId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class QueryDispatchEntityTest {

  private QueryDispatchId createId(Long queryId) {
    var id = new QueryDispatchId();
    id.setQueryId(queryId);
    id.setExternalId("external-" + queryId);
    id.setBrokerType(BrokerClientType.MOCK);
    return id;
  }

  @Test
  void testEquals_sameId_shouldBeEqual() {
    QueryDispatchEntity dispatch1 = new QueryDispatchEntity();
    dispatch1.setId(createId(1L));

    QueryDispatchEntity dispatch2 = new QueryDispatchEntity();
    dispatch2.setId(createId(1L));

    assertEquals(dispatch1, dispatch2);
    assertEquals(dispatch1.hashCode(), dispatch2.hashCode());
  }

  @Test
  void testEquals_differentId_shouldNotBeEqual() {
    QueryDispatchEntity dispatch1 = new QueryDispatchEntity();
    dispatch1.setId(createId(1L));

    QueryDispatchEntity dispatch2 = new QueryDispatchEntity();
    dispatch2.setId(createId(2L));

    assertNotEquals(dispatch1, dispatch2);
  }

  @Test
  void testEquals_nullId_shouldNotBeEqual() {
    QueryDispatchEntity dispatch1 = new QueryDispatchEntity(); // id is null
    QueryDispatchEntity dispatch2 = new QueryDispatchEntity(); // id is null

    assertNotEquals(dispatch1, dispatch2);
  }

  @Test
  void testEquals_self_shouldBeEqual() {
    QueryDispatchEntity dispatch = new QueryDispatchEntity();
    dispatch.setId(createId(1L));

    assertEquals(dispatch, dispatch);
  }

  @Test
  void testEquals_null_shouldNotBeEqual() {
    QueryDispatchEntity dispatch = new QueryDispatchEntity();
    dispatch.setId(createId(1L));

    assertNotEquals(null, dispatch);
  }

  @Test
  void testEquals_differentClass_shouldNotBeEqual() {
    QueryDispatchEntity dispatch = new QueryDispatchEntity();
    dispatch.setId(createId(1L));

    Object other = new Object();

    assertNotEquals(dispatch, other);
  }
}
