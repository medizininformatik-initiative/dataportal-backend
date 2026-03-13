package de.medizininformatikinitiative.dataportal.backend.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.query.api.DataExtraction;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.UpgradeIssue;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record DataExtractionUpgradeResult(
    @JsonProperty DataExtraction dataExtraction,
    @JsonProperty List<UpgradeIssue> upgradeIssues
    ) {
  public DataExtractionUpgradeResult {
    upgradeIssues = upgradeIssues == null ? List.of() : upgradeIssues;
  }
}
