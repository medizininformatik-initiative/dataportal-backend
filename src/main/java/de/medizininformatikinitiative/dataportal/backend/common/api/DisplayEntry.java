package de.medizininformatikinitiative.dataportal.backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.dse.api.LocalizedValue;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.Display;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileDisplay;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record DisplayEntry(
    @JsonProperty String original,
    @JsonProperty List<LocalizedValue> translations
) {

  public static DisplayEntry of(Display display) {
    return DisplayEntry.builder()
        .original(display.original())
        .translations(List.of(
            LocalizedValue.builder()
                .language("de-DE")
                .value(display.deDe())
                .build(),
            LocalizedValue.builder()
                .language("en-US")
                .value(display.enUs())
                .build()
        ))
        .build();
  }

  public static DisplayEntry of(ProfileDisplay display) {
    if (display == null) {
      return null;
    }
    return DisplayEntry.builder()
        .original(display.original())
        .translations(toLocalizedValues(display.translations()))
        .build();
  }

  /**
   * Turns a language-code-keyed translation map into a list of {@link LocalizedValue}s. "de-DE" and "en-US" are
   * always present (with a null value if missing) to keep the API shape stable; any additional languages present in
   * the source data are appended in alphabetical order.
   */
  public static List<LocalizedValue> toLocalizedValues(Map<String, String> translations) {
    var languages = new TreeSet<String>();
    languages.add("de-DE");
    languages.add("en-US");
    if (translations != null) {
      languages.addAll(translations.keySet());
    }
    return languages.stream()
        .map(language -> LocalizedValue.builder()
            .language(language)
            .value(translations == null ? null : translations.get(language))
            .build())
        .toList();
  }
}
