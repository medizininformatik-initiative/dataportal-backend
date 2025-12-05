package de.medizininformatikinitiative.dataportal.backend.common.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TermCodeTest {

  @Test
  public void testEqualsAndHashCode() {
    TermCode termCode1 = TermCode.builder()
        .code("code1")
        .system("system1")
        .version("version1")
        .display("display1")
        .build();

    TermCode termCode2 = TermCode.builder()
        .code("code1")
        .system("system1")
        .version("version2")
        .display("display2")
        .build();

    TermCode termCode3 = TermCode.builder()
        .code("code2")
        .system("system2")
        .version("version1")
        .display("display1")
        .build();

    assertEquals(termCode1, termCode2);
    assertNotEquals(termCode1, termCode3);

    assertEquals(termCode1.hashCode(), termCode2.hashCode());
    assertNotEquals(termCode1.hashCode(), termCode3.hashCode());

    assertNotEquals(termCode1, null);
    assertNotEquals(termCode1, "");
  }
}
