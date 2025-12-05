package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.ALWAYS)
public enum ValueDefinitonType {
  @JsonProperty("concept")
  CONCEPT,
  @JsonProperty("quantity")
  QUANTITY,
  @JsonProperty("reference")
  REFERENCE
}
