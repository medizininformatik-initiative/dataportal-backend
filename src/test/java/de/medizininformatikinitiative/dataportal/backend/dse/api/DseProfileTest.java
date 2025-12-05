package de.medizininformatikinitiative.dataportal.backend.dse.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DseProfileTest {

  @Test
  void testConstructorWithValues() {
    DseProfile dseProfile = DseProfile.builder()
        .url("url")
        .display(DisplayEntry.builder().build())
        .module(DisplayEntry.builder().build())
        .fields(List.of(Field.builder().build()))
        .filters(List.of(Filter.builder().build()))
        .references(List.of(Reference.builder().build()))
        .errorCode("error")
        .errorCause("123")
        .build();

    assertNotNull(dseProfile);
    assertNotNull(dseProfile.url());
    assertNotNull(dseProfile.display());
    assertNotNull(dseProfile.module());
    assertNotNull(dseProfile.fields());
    assertNotNull(dseProfile.filters());
    assertNotNull(dseProfile.references());
    assertNotNull(dseProfile.errorCode());
    assertNotNull(dseProfile.errorCause());

    assertNotEquals(dseProfile.fields(), Collections.emptyList());
    assertNotEquals(dseProfile.filters(), Collections.emptyList());
    assertNotEquals(dseProfile.references(), Collections.emptyList());
  }

  @Test
  public void testConstructorWithNull() {
    DseProfile dseProfile = DseProfile.builder()
        .url("url")
        .display(DisplayEntry.builder().build())
        .module(DisplayEntry.builder().build())
        .fields(null)
        .filters(null)
        .references(null)
        .errorCode("error")
        .errorCause("123")
        .build();

    assertNotNull(dseProfile);
    assertNotNull(dseProfile.url());
    assertNotNull(dseProfile.display());
    assertNotNull(dseProfile.module());
    assertNotNull(dseProfile.fields());
    assertNotNull(dseProfile.filters());
    assertNotNull(dseProfile.references());
    assertNotNull(dseProfile.errorCode());
    assertNotNull(dseProfile.errorCause());

    assertEquals(dseProfile.fields(), Collections.emptyList());
    assertEquals(dseProfile.filters(), Collections.emptyList());
    assertEquals(dseProfile.references(), Collections.emptyList());
  }

}
