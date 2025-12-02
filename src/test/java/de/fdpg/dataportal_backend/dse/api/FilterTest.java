package de.fdpg.dataportal_backend.dse.api;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilterTest {

  @Test
  void testConstructorWithValues() {
    Filter filter = Filter.builder()
        .type("filter")
        .name("filter")
        .uiType("uiType")
        .valueSetUrls(List.of("url1", "url2"))
        .build();

    assertNotNull(filter);
    assertNotNull(filter.valueSetUrls());
    assertNotEquals(Collections.emptyList(), filter.valueSetUrls());
  }

  @Test
  public void testConstructorWithNull() {
    Filter filter = Filter.builder()
        .type(null)
        .name(null)
        .uiType(null)
        .valueSetUrls(null)
        .build();

    assertNotNull(filter);
    assertNotNull(filter.valueSetUrls());
    assertEquals(Collections.emptyList(), filter.valueSetUrls());
  }

}
