package de.medizininformatikinitiative.dataportal.backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record DseProfile(
    @JsonProperty String url,
    @JsonProperty DisplayEntry display,
    @JsonProperty DisplayEntry module,
    @JsonProperty List<Field> fields,
    @JsonProperty List<Filter> filters,
    @JsonProperty List<Reference> references,
    @JsonProperty String errorCode,
    @JsonProperty String errorCause
) {
  public DseProfile {
    fields = (fields == null) ? List.of() : fields;
    filters = (filters == null) ? List.of() : filters;
    references = (references == null) ? List.of() : references;
  }
}
