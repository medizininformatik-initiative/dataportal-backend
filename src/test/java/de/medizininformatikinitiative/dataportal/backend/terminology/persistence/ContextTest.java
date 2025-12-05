package de.medizininformatikinitiative.dataportal.backend.terminology.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ContextTest {

  @Test
  void testEqualsAndHashCode_sameContext() {
    var context = createContext(1L, "some code", "some display", "some version", "some system");

    assertEquals(context, context);
    assertEquals(context.hashCode(), context.hashCode());
  }

  @Test
  void testEqualsAndHashCode_equalContext() {
    var context1 = createContext(1L, "some code", "some display", "some version", "some system");
    var context2 = createContext(1L, "some code", "some display", "some version", "some system");

    assertEquals(context1, context2);
    assertEquals(context2, context1);
    assertEquals(context1.hashCode(), context2.hashCode());
  }

  @Test
  void testEqualsAndHashCode_differentContext() {
    var context1 = createContext(1L, "some code", "some display", "some version", "some system");
    var context2 = createContext(2L, "another code", "another display", "another version", "another system");

    assertNotEquals(context1, context2);
    assertNotEquals(context2, context1);
    assertNotEquals(context1.hashCode(), context2.hashCode());
  }

  @Test
  void testEquals_nullObjectNotEquals() {
    var context = createContext(1L, "some code", "some display", "some version", "some system");

    assertNotEquals(context, null);
    assertNotEquals(null, context);
  }

  @Test
  void testEqualsAndHashCode_otherClassNotEquals() {
    var context = createContext(1L, "some code", "some display", "some version", "some system");
    Integer i = 10;

    assertNotEquals(context, i);
    assertNotEquals(context.hashCode(), i.hashCode());
  }

  private Context createContext(Long id, String code, String display, String version, String system) {
    var context = new Context();
    context.setId(id);
    context.setCode(code);
    context.setDisplay(display);
    context.setVersion(version);
    context.setSystem(system);
    return context;
  }
}
