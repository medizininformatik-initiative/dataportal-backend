package de.medizininformatikinitiative.dataportal.backend.query.broker.direct;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Component
@Slf4j
public class FhirConnector {

  private final IGenericClient client;

  public FhirConnector(IGenericClient client) {
    this.client = client;
  }

  /**
   * Submit a {@link Bundle} to the FHIR server.
   *
   * @param bundle the {@link Bundle} to submit
   * @throws IOException if the communication with the FHIR server fails due to any client or server error
   */
  public void transmitBundle(Bundle bundle) throws IOException {
    try {
      client.transaction().withBundle(bundle).execute();
    } catch (BaseServerResponseException e) {
      throw new IOException("An error occurred while trying to create measure and library", e);
    }
  }

  /**
   * Get the {@link MeasureReport} for a previously transmitted {@link Measure}
   *
   * @param measureUri the identifier of the {@link Measure}
   * @return the retrieved {@link MeasureReport} from the server
   * @throws IOException if the communication with the FHIR server fails due to any client/server error or the response
   *                     did not contain a {@link MeasureReport}
   */
  public MeasureReport evaluateMeasure(String measureUri) throws IOException {
    try {
      var response = client.operation()
          .onType(Measure.class)
          .named("evaluate-measure")
          .withSearchParameter(Parameters.class, "measure", new StringParam(measureUri))
          .andSearchParameter("periodStart", new DateParam("1900"))
          .andSearchParameter("periodEnd", new DateParam("2100"))
          .useHttpGet()
          .preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class))
          .execute();

      var result = Optional.of(response)
          .filter(Parameters::hasParameter)
          .map(Parameters::getParameterFirstRep)
          .filter(ParametersParameterComponent::hasResource)
          .map(ParametersParameterComponent::getResource)
          .map(this::extractResult)
          .orElseThrow(() -> new IOException("An error occurred while trying to evaluate a measure report"));

      if (result.hasReport()) {
        return result.report();
      }

      if (result.outcome() != null) {
        String diagnostics = result.outcome().getIssue().stream()
            .map(OperationOutcome.OperationOutcomeIssueComponent::getDiagnostics)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("No diagnostics provided");

        throw new IOException("Measure evaluation failed: " + diagnostics);
      }

      throw new IOException("Measure evaluation failed with unknown error");

    } catch (BaseServerResponseException e) {
      throw new IOException("An error occurred while trying to evaluate a measure report", e);
    }
  }

  private MeasureEvaluationResult extractResult(Resource r) {
    if (r instanceof MeasureReport mr) {
      return new MeasureEvaluationResult(mr, null);
    } else if (r instanceof Bundle bundle) {
      return bundle.getEntry().stream()
          .map(Bundle.BundleEntryComponent::getResource)
          .filter(Objects::nonNull)
          .findFirst()
          .map(this::extractResult)
          .orElse(new MeasureEvaluationResult(null, null));
    } else if (r instanceof OperationOutcome outcome) {
      log.error("Operation failed: {}", outcome.getIssueFirstRep().getDiagnostics());
      return new MeasureEvaluationResult(null, outcome);
    } else {
      log.error("Response contains unexpected resource type: {}", r.getClass().getSimpleName());
      return new MeasureEvaluationResult(null, null);
    }
  }
}
