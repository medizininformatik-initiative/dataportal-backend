package de.medizininformatikinitiative.dataportal.backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.dse.api.LocalizedValue;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.Display;
import lombok.Builder;

import java.util.List;

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
}
