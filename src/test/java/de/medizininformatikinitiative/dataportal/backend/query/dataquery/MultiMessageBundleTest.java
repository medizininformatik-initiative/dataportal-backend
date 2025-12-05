package de.medizininformatikinitiative.dataportal.backend.query.dataquery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class MultiMessageBundleTest {

  private static final String EXISTING_KEY = "conjunctionGroup";
  private static final String NON_EXISTENT_KEY = "unknownEntry";

  @Test
  void getEntry_succeedsEnglish() {
    String result = MultiMessageBundle.getEntry(EXISTING_KEY, DataqueryCsvExportService.SUPPORTED_LANGUAGES.EN);
    assertNotNull(result);
    assertNotEquals(EXISTING_KEY + "(translation not found)", result);
  }

  @Test
  void getEntry_succeedsGerman() {
    String result = MultiMessageBundle.getEntry(EXISTING_KEY, DataqueryCsvExportService.SUPPORTED_LANGUAGES.DE);
    assertNotNull(result);
    assertNotEquals(EXISTING_KEY + "(translation not found)", result);
  }

  @Test
  void getEntry_notFoundEnglish() {
    String result = MultiMessageBundle.getEntry(NON_EXISTENT_KEY, DataqueryCsvExportService.SUPPORTED_LANGUAGES.EN);
    assertEquals(NON_EXISTENT_KEY + "(translation not found)", result);
  }

  @Test
  void getEntry_notFoundGerman() {
    String result = MultiMessageBundle.getEntry(NON_EXISTENT_KEY, DataqueryCsvExportService.SUPPORTED_LANGUAGES.DE);
    assertEquals(NON_EXISTENT_KEY + "(translation not found)", result);
  }

  @Test
  void getEntry_noLanguage() {
    String result = MultiMessageBundle.getEntry(EXISTING_KEY, null);
    assertEquals(EXISTING_KEY + "(translation not found)", result);
  }
}
