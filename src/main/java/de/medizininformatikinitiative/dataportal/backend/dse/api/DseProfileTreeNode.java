package de.medizininformatikinitiative.dataportal.backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record DseProfileTreeNode(
    @JsonProperty String id,
    @JsonProperty List<DseProfileTreeNode> children,
    @JsonProperty String name,
    @JsonProperty DisplayEntry display,
    @JsonProperty FieldDisplayEntry fields,
    @JsonProperty String module,
    @JsonProperty String url,
    @JsonProperty boolean leaf,
    @JsonProperty boolean selectable
) {
  public DseProfileTreeNode {
    children = (children == null) ? List.of() : children;
  }
}
