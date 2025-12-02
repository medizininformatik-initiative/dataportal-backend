package de.fdpg.dataportal_backend.query.api;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.fdpg.dataportal_backend.common.api.TermCode;
import lombok.Builder;

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
