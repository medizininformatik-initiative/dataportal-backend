package de.fdpg.dataportal_backend.dse.api;

import de.fdpg.dataportal_backend.common.api.DisplayEntry;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FieldTest {

  @Test
  void testConstructorWithValues() {
    Field child = Field.builder()
        .description(DisplayEntry.builder().build())
        .display(DisplayEntry.builder().build())
        .recommended(true)
        .id("childid")
        .required(false)
        .children(null)
        .build();

    Field field = Field.builder()
        .description(DisplayEntry.builder().build())
        .display(DisplayEntry.builder().build())
        .recommended(true)
        .id("id")
        .required(false)
        .children(List.of(child))
        .build();

    assertNotNull(field);
    assertEquals("id", field.id());
    assertEquals("childid", field.children().get(0).id());
  }

  @Test
  public void testConstructorWithNull() {
    Field field = Field.builder()
        .description(DisplayEntry.builder().build())
        .display(DisplayEntry.builder().build())
        .recommended(true)
        .id("id")
        .required(false)
        .children(null)
        .build();

    assertNotNull(field);
    assertNotNull(field.children());
    assertEquals(field.children(), Collections.emptyList());
  }
}
