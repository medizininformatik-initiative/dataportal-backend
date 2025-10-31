package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(Include.ALWAYS)
public record UiProfileEntry(
    @JsonProperty("id") String id,
    @JsonProperty("uiProfile") UiProfile uiProfile
) {
  public static UiProfileEntry of(UiProfile uiProfile) {
    return UiProfileEntry.builder()
        .id(uiProfile.name())
        .uiProfile(uiProfile)
        .build();
  }
}
