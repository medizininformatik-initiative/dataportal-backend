package de.medizininformatikinitiative.dataportal.backend.query.dataquery;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class MultiMessageBundle {

  private static final Map<DataqueryCsvExportService.SUPPORTED_LANGUAGES, ResourceBundle> bundles = new HashMap<>();

  static {
    bundles.put(DataqueryCsvExportService.SUPPORTED_LANGUAGES.EN, ResourceBundle.getBundle("de.medizininformatikinitiative.dataportal.backend.query.dataquery.CsvMessages", Locale.ENGLISH));
    bundles.put(DataqueryCsvExportService.SUPPORTED_LANGUAGES.DE, ResourceBundle.getBundle("de.medizininformatikinitiative.dataportal.backend.query.dataquery.CsvMessages", Locale.GERMAN));
  }

  public static String getEntry(String key, DataqueryCsvExportService.SUPPORTED_LANGUAGES language) {
    ResourceBundle bundle = bundles.get(language);
    if (bundle != null && bundle.containsKey(key)) {
      return bundle.getString(key);
    } else {
      return key + "(translation not found)";
    }
  }
}
