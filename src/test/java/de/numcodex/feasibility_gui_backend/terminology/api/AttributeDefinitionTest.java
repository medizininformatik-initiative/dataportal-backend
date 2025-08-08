package de.numcodex.feasibility_gui_backend.terminology.api;

import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttributeDefinitionTest {

  @Test
  void testConstructorWithValues() {
    AttributeDefinition attributeDefinition = AttributeDefinition.builder()
        .display(null)
        .type(null)
        .selectableConcepts(List.of(TermCode.builder().build()))
        .attributeCode(null)
        .comparator(null)
        .optional(false)
        .allowedUnits(List.of(TermCode.builder().build()))
        .precision(0.0)
        .min(0.0)
        .max(0.0)
        .referencedCriteriaSets(null)
        .referencedValueSets(null)
        .build();

    assertNotNull(attributeDefinition);
    assertNotNull(attributeDefinition.selectableConcepts());
    assertNotNull(attributeDefinition.allowedUnits());
    assertNotEquals(Collections.EMPTY_LIST, attributeDefinition.selectableConcepts());
    assertNotEquals(Collections.EMPTY_LIST, attributeDefinition.allowedUnits());
  }

  @Test
  public void testConstructorWithNull() {
    AttributeDefinition attributeDefinition = AttributeDefinition.builder()
        .display(null)
        .type(null)
        .selectableConcepts(null)
        .attributeCode(null)
        .comparator(null)
        .optional(false)
        .allowedUnits(null)
        .precision(0.0)
        .min(0.0)
        .max(0.0)
        .referencedCriteriaSets(null)
        .referencedValueSets(null)
      .build();

    assertNotNull(attributeDefinition);
    assertNotNull(attributeDefinition.selectableConcepts());
    assertNotNull(attributeDefinition.allowedUnits());
    assertEquals(Collections.EMPTY_LIST, attributeDefinition.selectableConcepts());
    assertEquals(Collections.EMPTY_LIST, attributeDefinition.allowedUnits());
  }
}
