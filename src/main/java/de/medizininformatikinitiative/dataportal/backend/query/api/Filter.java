package de.medizininformatikinitiative.dataportal.backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.common.api.TermCode;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Filter(
    @JsonProperty String type,
    @JsonProperty String name,
    @JsonProperty List<TermCode> codes,
    @JsonProperty LocalDate start,
    @JsonProperty LocalDate end
) {
  public Filter {
    codes = codes == null ? List.of() : codes;
  }
}
