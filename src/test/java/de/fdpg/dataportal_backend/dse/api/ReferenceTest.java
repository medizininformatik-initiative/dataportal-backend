package de.fdpg.dataportal_backend.dse.api;

import de.fdpg.dataportal_backend.common.api.DisplayEntry;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceTest {

  @Test
  void testConstructorWithValues() {
    Reference childReference = Reference.builder()
        .id("childid")
        .display(null)
        .description(null)
        .type(null)
        .recommended(false)
        .required(false)
        .referencedProfiles(null)
        .children(null)
        .build();

    ReferencedProfile referencedProfile = ReferencedProfile.builder()
        .url(null)
        .display(null)
        .fields(null)
        .build();

    Reference reference = Reference.builder()
        .id("id")
        .display(DisplayEntry.builder().build())
        .description(DisplayEntry.builder().build())
        .type("type")
        .recommended(true)
        .required(true)
        .referencedProfiles(List.of(referencedProfile))
        .children(List.of(childReference))
        .build();

    assertNotNull(reference);
    assertNotNull(reference.referencedProfiles());
    assertNotNull(reference.children());
    assertNotEquals(Collections.emptyList(), reference.referencedProfiles());
    assertNotEquals(Collections.emptyList(), reference.children());
    assertEquals(childReference.id(), reference.children().get(0).id());
  }

  @Test
  public void testConstructorWithNull() {
    Reference reference = Reference.builder()
        .id(null)
        .display(null)
        .description(null)
        .type(null)
        .recommended(false)
        .required(false)
        .referencedProfiles(null)
        .children(null)
        .build();

    assertNotNull(reference);
    assertNotNull(reference.referencedProfiles());
    assertNotNull(reference.children());
    assertEquals(Collections.emptyList(), reference.referencedProfiles());
    assertEquals(Collections.emptyList(), reference.children());
  }

}
