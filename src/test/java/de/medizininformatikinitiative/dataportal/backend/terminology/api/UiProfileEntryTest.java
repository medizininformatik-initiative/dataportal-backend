package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.Comparator;
import de.medizininformatikinitiative.dataportal.backend.common.api.TermCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UiProfileEntryTest {

  @Test
  void testBuilder() {
    UiProfile dummyUiProfile = createUiProfile();
    UiProfileEntry entry = UiProfileEntry.builder()
        .id(dummyUiProfile.name())
        .uiProfile(dummyUiProfile)
        .build();

    assertNotNull(entry);
    assertEquals(dummyUiProfile.name(), entry.id());
    assertEquals(dummyUiProfile, entry.uiProfile());
  }

  @Test
  void testOfMethod() {
    UiProfile dummyUiProfile = createUiProfile();
    UiProfileEntry entry = UiProfileEntry.of(dummyUiProfile);

    assertNotNull(entry);
    assertEquals(dummyUiProfile.name(), entry.id());
    assertEquals(dummyUiProfile, entry.uiProfile());
  }

  private UiProfile createUiProfile() {
    return UiProfile.builder()
        .name("test-profile")
        .attributeDefinitions(List.of(createAttributeDefinition()))
        .valueDefinition(createAttributeDefinition())
        .timeRestrictionAllowed(true)
        .build();
  }

  private AttributeDefinition createAttributeDefinition() {
    return AttributeDefinition.builder()
        .min(1.0)
        .max(99.9)
        .allowedUnits(List.of(createTermCode()))
        .attributeCode(createTermCode())
        .type(ValueDefinitonType.CONCEPT)
        .optional(false)
        .referencedCriteriaSets(List.of("http://my.reference.criteria/set"))
        .referencedValueSets(List.of("http://my.reference.value/set"))
        .comparator(Comparator.EQUAL)
        .precision(1.0)
        .selectableConcepts(List.of(createTermCode()))
        .build();
  }

  private TermCode createTermCode() {
    return TermCode.builder()
        .code("LL2191-6")
        .system("http://loinc.org")
        .display("Geschlecht")
        .version("1.0.0")
        .build();
  }
}
