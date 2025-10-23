package de.medizininformatikinitiative.dataportal.backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.common.api.Criterion;
import de.medizininformatikinitiative.dataportal.backend.common.api.MutableCriterion;
import de.medizininformatikinitiative.dataportal.backend.query.api.validation.CcdlValidation;
import lombok.Builder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(Include.NON_EMPTY)
@CcdlValidation
@Builder
public record Ccdl(
    @JsonProperty URI version,
    @JsonProperty("inclusionCriteria") List<List<Criterion>> inclusionCriteria,
    @JsonProperty("exclusionCriteria") List<List<Criterion>> exclusionCriteria,
    @JsonProperty("display") String display
) {
  public static Ccdl createImmutableCcdl(MutableCcdl mutableCcdl) {
    List<List<Criterion>> inclusionCriteria = new ArrayList<>();
    for (List<MutableCriterion> outerList : mutableCcdl.getInclusionCriteria()) {
      List<Criterion> innerList = new ArrayList<>();
      for (MutableCriterion criterion : outerList) {
        innerList.add(Criterion.createImmutableCriterion(criterion));
      }
      inclusionCriteria.add(innerList);
    }

    List<List<Criterion>> exclusionCriteria = new ArrayList<>();
    for (List<MutableCriterion> outerList : mutableCcdl.getExclusionCriteria()) {
      List<Criterion> innerList = new ArrayList<>();
      for (MutableCriterion criterion : outerList) {
        innerList.add(Criterion.createImmutableCriterion(criterion));
      }
      exclusionCriteria.add(innerList);
    }

    return Ccdl.builder()
        .version(mutableCcdl.getVersion())
        .display(mutableCcdl.getDisplay())
        .inclusionCriteria(inclusionCriteria)
        .exclusionCriteria(exclusionCriteria)
        .build();
  }
}
