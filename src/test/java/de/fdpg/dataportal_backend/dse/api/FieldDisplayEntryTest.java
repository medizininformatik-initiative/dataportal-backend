package de.fdpg.dataportal_backend.dse.api;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FieldDisplayEntryTest {

  @Test
  public void testConstructorWithValues() {
    List<String> original = List.of("original-value1",  "original-value2");
    List<LocalizedValueList> translations = List.of(LocalizedValueList.builder()
        .language("de-De")
        .value(List.of("value-value"))
        .build());

    FieldDisplayEntry fieldDisplayEntry = FieldDisplayEntry.builder()
        .original(original)
        .translations(translations)
        .build();

    assertNotNull(fieldDisplayEntry);
    assertEquals(fieldDisplayEntry.original(), original);
    assertEquals(fieldDisplayEntry.translations().size(), translations.size());
    assertEquals(fieldDisplayEntry.translations().get(0), translations.get(0));
  }

  @Test
  public void testConstructorWithNulls() {
    FieldDisplayEntry fieldDisplayEntry = FieldDisplayEntry.builder()
        .original(null)
        .translations(null)
        .build();

    assertNotNull(fieldDisplayEntry);
    assertNotNull(fieldDisplayEntry.original());
    assertNotNull(fieldDisplayEntry.translations());
    assertEquals(fieldDisplayEntry.original(), Collections.emptyList());
    assertEquals(fieldDisplayEntry.translations(), Collections.emptyList());
  }

}
