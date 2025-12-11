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
public class MutableCcdl {
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

  public static MutableCcdl createMutableCcdl(@NonNull Ccdl ccdl) {
    List<List<MutableCriterion>> mutableInclusionCriteria = new ArrayList<>();
    if (ccdl.inclusionCriteria() != null) {
      for (List<Criterion> outerList : ccdl.inclusionCriteria()) {
        List<MutableCriterion> innerList = new ArrayList<>();
        for (Criterion criterion : outerList) {
          innerList.add(MutableCriterion.createMutableCriterion(criterion));
        }
        mutableInclusionCriteria.add(innerList);
      }
    }

    List<List<MutableCriterion>> mutableExclusionCriteria = new ArrayList<>();
    if (ccdl.exclusionCriteria() != null) {
      for (List<Criterion> outerList : ccdl.exclusionCriteria()) {
        List<MutableCriterion> innerList = new ArrayList<>();
        for (Criterion criterion : outerList) {
          innerList.add(MutableCriterion.createMutableCriterion(criterion));
        }
        mutableExclusionCriteria.add(innerList);
      }
    }
    return MutableCcdl.builder()
        .version(ccdl.version())
        .inclusionCriteria(mutableInclusionCriteria)
        .exclusionCriteria(mutableExclusionCriteria)
        .display(ccdl.display())
        .build();
  }
}
