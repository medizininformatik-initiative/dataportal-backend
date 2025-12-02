package de.fdpg.dataportal_backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record FieldDisplayEntry(
    @JsonProperty List<String> original,
    @JsonProperty List<LocalizedValueList> translations
) {
  public FieldDisplayEntry {
    original = (original == null) ? List.of() : original;
    translations = (translations == null) ? List.of() : translations;
  }
}
