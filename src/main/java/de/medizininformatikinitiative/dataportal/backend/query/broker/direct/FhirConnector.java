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
      return Optional
          .of(client.operation()
              .onType(Measure.class)
              .named("evaluate-measure")
              .withSearchParameter(Parameters.class, "measure", new StringParam(measureUri))
              .andSearchParameter("periodStart", new DateParam("1900"))
              .andSearchParameter("periodEnd", new DateParam("2100"))
              .useHttpGet()
              .preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class))
              .execute())
          .filter(Parameters::hasParameter)
          .map(Parameters::getParameterFirstRep)
          .filter(ParametersParameterComponent::hasResource)
          .map(ParametersParameterComponent::getResource)
          .flatMap(this::toMeasureReport)
          .orElseThrow(() -> new IOException("An error occurred while trying to evaluate a measure report"));
    } catch (BaseServerResponseException e) {
      throw new IOException("An error occurred while trying to evaluate a measure report", e);
    }
  }

  private Optional<MeasureReport> toMeasureReport(Resource r) {
    if (r instanceof MeasureReport) {
      return Optional.of((MeasureReport) r);
    } else if (r instanceof Bundle) {
      var report = Optional.of((Bundle) r)
          .filter(Bundle::hasEntry)
          .map(Bundle::getEntryFirstRep)
          .filter(Bundle.BundleEntryComponent::hasResource)
          .map(Bundle.BundleEntryComponent::getResource)
          .filter(MeasureReport.class::isInstance)
          .map(MeasureReport.class::cast);
      if (report.isEmpty()) {
        log.error("Failed to extract MeasureReport from Bundle");
      }
      return report;
    } else if (r instanceof OperationOutcome) {
      log.error("Operation failed: {}", ((OperationOutcome) r).getIssueFirstRep().getDiagnostics());
      return Optional.empty();
    } else {
      log.error("Response contains unexpected resource type: {}", r.getClass().getSimpleName());
      return Optional.empty();
    }
  }
}
