package de.medizininformatikinitiative.dataportal.backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.common.api.Criterion;
import de.medizininformatikinitiative.dataportal.backend.common.api.MutableCriterion;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(Include.NON_EMPTY)
@Builder
@Data
public class MutableStructuredQuery {
  @JsonProperty
  URI version;

  @JsonProperty("inclusionCriteria")
  @Builder.Default
  List<List<MutableCriterion>> inclusionCriteria = List.of(List.of());

  @JsonProperty("exclusionCriteria")
  @Builder.Default
  List<List<MutableCriterion>> exclusionCriteria = List.of(List.of());

  @JsonProperty("display")
  String display;

  public static MutableStructuredQuery createMutableStructuredQuery(@NonNull StructuredQuery structuredQuery) {
    List<List<MutableCriterion>> mutableInclusionCriteria = new ArrayList<>();
    if (structuredQuery.inclusionCriteria() != null) {
      for (List<Criterion> outerList : structuredQuery.inclusionCriteria()) {
        List<MutableCriterion> innerList = new ArrayList<>();
        for (Criterion criterion : outerList) {
          innerList.add(MutableCriterion.createMutableCriterion(criterion));
        }
        mutableInclusionCriteria.add(innerList);
      }
    }

    List<List<MutableCriterion>> mutableExclusionCriteria = new ArrayList<>();
    if (structuredQuery.exclusionCriteria() != null) {
      for (List<Criterion> outerList : structuredQuery.exclusionCriteria()) {
        List<MutableCriterion> innerList = new ArrayList<>();
        for (Criterion criterion : outerList) {
          innerList.add(MutableCriterion.createMutableCriterion(criterion));
        }
        mutableExclusionCriteria.add(innerList);
      }
    }
    return MutableStructuredQuery.builder()
        .version(structuredQuery.version())
        .inclusionCriteria(mutableInclusionCriteria)
        .exclusionCriteria(mutableExclusionCriteria)
        .display(structuredQuery.display())
        .build();
  }
}
