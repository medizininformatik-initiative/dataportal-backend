package de.medizininformatikinitiative.dataportal.backend.query.broker.direct;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.OperationOutcome;

public record MeasureEvaluationResult(
    MeasureReport report,
    OperationOutcome outcome
) {
  boolean hasReport() {
    return report != null;
  }
}