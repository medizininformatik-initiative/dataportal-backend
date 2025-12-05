package de.medizininformatikinitiative.dataportal.backend.dse.api;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalizedValueListTest {

  @Test
  void testConstructorWithValues() {
    LocalizedValueList localizedValueList = LocalizedValueList.builder()
        .language("de-DE")
        .value(List.of("value1", "value2"))
        .build();

    assertNotNull(localizedValueList);
    assertNotNull(localizedValueList.value());
    assertNotEquals(Collections.emptyList(), localizedValueList.value());
  }

  @Test
  public void testConstructorWithNull() {
    LocalizedValueList localizedValueList = LocalizedValueList.builder()
        .language(null)
        .value(null)
        .build();

    assertNotNull(localizedValueList);
    assertNotNull(localizedValueList.value());
    assertEquals(Collections.emptyList(), localizedValueList.value());
  }
}
