package de.medizininformatikinitiative.dataportal.backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record Reference(
    @JsonProperty String id,
    @JsonProperty DisplayEntry display,
    @JsonProperty DisplayEntry description,
    @JsonProperty String type,
    @JsonProperty boolean recommended,
    @JsonProperty boolean required,
    @JsonProperty List<ReferencedProfile> referencedProfiles,
    @JsonProperty List<Reference> children
) {
  public Reference {
    referencedProfiles = (referencedProfiles == null) ? List.of() : referencedProfiles;
    children = (children == null) ? List.of() : children;
  }
}
