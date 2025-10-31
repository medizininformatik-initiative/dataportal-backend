package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import lombok.Builder;

import java.util.List;
import java.util.Objects;

@Builder
@JsonInclude(Include.ALWAYS)
public record CriteriaProfileData(
    @JsonProperty("id") String id,
    @JsonProperty("display") DisplayEntry display,
    @JsonProperty("context") TermCode context,
    @JsonProperty("termCodes") List<TermCode> termCodes,
    @JsonProperty("uiProfile") String uiProfile
) {
    public CriteriaProfileData {
        termCodes = termCodes == null ? List.of() : termCodes;
    }

    public CriteriaProfileData addDisplay(DisplayEntry newDisplay) {
        return CriteriaProfileData.builder()
            .id(id)
            .display(newDisplay)
            .context(context)
            .termCodes(termCodes)
            .uiProfile(uiProfile)
            .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CriteriaProfileData that = (CriteriaProfileData) o;
        return Objects.equals(termCodes, that.termCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(termCodes);
    }

}
