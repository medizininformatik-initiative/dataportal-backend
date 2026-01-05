package de.medizininformatikinitiative.dataportal.backend.query.broker.direct;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementSoftwareComponent;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
public class FhirConnectorTest {


  private static final String MEASURE_URI = "uri:1-measure-example-uri";
  private static final int MEASURE_COUNT = 8723132;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  IGenericClient client;
  FhirConnector fhirConnector;
  @Mock
  private IOperationUntypedWithInput<Parameters> operation;

  @BeforeEach
  void setUp() {
    fhirConnector = new FhirConnector(client);
  }

  @Test
  void testTransmitBundleSuccess() throws Exception {
    Bundle bundle = new Bundle();

    fhirConnector.transmitBundle(bundle);

    verify(client.transaction().withBundle(bundle)).execute();
  }


  @Test
  void testTransmitBundleIOException() {
    Bundle bundle = new Bundle();

    BaseServerResponseException e = BaseServerResponseException.newInstance(200, "test");
    Mockito.when(client.transaction().withBundle(bundle).execute()).thenThrow(e);

    assertThatThrownBy(() -> fhirConnector.transmitBundle(bundle))
        .isInstanceOf(IOException.class);
  }

  @Test
  void testEvaluateMeasureSuccess() throws Exception {
    var measureReport = new MeasureReport();
    measureReport.addGroup().addPopulation().setCount(MEASURE_COUNT);
    var paramerters = new Parameters()
        .addParameter(new ParametersParameterComponent()
            .setResource(measureReport));

    when(client.operation()
        .onType(Measure.class)
        .named("evaluate-measure")
        .withSearchParameter(Parameters.class, "measure", new StringParam(MEASURE_URI))
        .andSearchParameter("periodStart", new DateParam("1900"))
        .andSearchParameter("periodEnd", new DateParam("2100"))
        .useHttpGet())
        .thenReturn(operation);
    when(operation.preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class)))
        .thenReturn(operation);
    when(operation
        .execute()).thenReturn(paramerters);

    assertThat(fhirConnector.evaluateMeasure(MEASURE_URI).getGroup().get(0).getPopulationFirstRep().getCount())
        .isEqualTo(MEASURE_COUNT);

  }

  @Test
  @DisplayName("Result of evaluating measure is a Bundle containing the MeasureReport and gets processed correctly")
  void testEvaluateMeasureSucceedsWithMeasureReportInBundle() throws Exception {
    var measureReport = new MeasureReport();
    var bundle = new Bundle().setEntry(List.of(new Bundle.BundleEntryComponent().setResource(measureReport)));
    measureReport.addGroup().addPopulation().setCount(MEASURE_COUNT);
    var paramerters = new Parameters()
        .addParameter(new ParametersParameterComponent()
            .setResource(bundle));

    when(client.operation()
        .onType(Measure.class)
        .named("evaluate-measure")
        .withSearchParameter(Parameters.class, "measure", new StringParam(MEASURE_URI))
        .andSearchParameter("periodStart", new DateParam("1900"))
        .andSearchParameter("periodEnd", new DateParam("2100"))
        .useHttpGet())
        .thenReturn(operation);
    when(operation.preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class)))
        .thenReturn(operation);
    when(operation
        .execute()).thenReturn(paramerters);

    assertThat(fhirConnector.evaluateMeasure(MEASURE_URI).getGroup().get(0).getPopulationFirstRep().getCount())
        .isEqualTo(MEASURE_COUNT);

  }

  @Test
  @DisplayName("OperationOutcome issue message of failed operation gets logged")
  void testEvaluateMeasureFailsWithOperationOutcome(CapturedOutput output) throws Exception {
    var issueMessage = "foobar-042104";
    var outcome = new OperationOutcome()
        .setIssue(List.of(new OperationOutcome.OperationOutcomeIssueComponent().setDiagnostics(issueMessage)));
    var parameters = new Parameters()
        .addParameter(new ParametersParameterComponent()
            .setResource(outcome));

    when(client.operation()
        .onType(Measure.class)
        .named("evaluate-measure")
        .withSearchParameter(Parameters.class, "measure", new StringParam(MEASURE_URI))
        .andSearchParameter("periodStart", new DateParam("1900"))
        .andSearchParameter("periodEnd", new DateParam("2100"))
        .useHttpGet())
        .thenReturn(operation);
    when(operation.preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class)))
        .thenReturn(operation);
    when(operation
        .execute()).thenReturn(parameters);

    assertThatThrownBy(() -> fhirConnector.evaluateMeasure(MEASURE_URI))
        .isInstanceOf(IOException.class);
    assertThat(output.getOut()).contains(issueMessage);

  }

  @Test
  @DisplayName("evaluating measure returns Bundle with unknown resource type entry")
  void testEvaluateMeasureFailsWithBundleContainingInvalidResourceType(CapturedOutput output) throws Exception {
    var resource = new CapabilityStatement()
        .setSoftware(new CapabilityStatementSoftwareComponent(new StringType("name-073450")));
    var bundle = new Bundle().addEntry(new Bundle.BundleEntryComponent().setResource(resource));
    var paramerters = new Parameters()
        .addParameter(new ParametersParameterComponent()
            .setResource(bundle));

    when(client.operation()
        .onType(Measure.class)
        .named("evaluate-measure")
        .withSearchParameter(Parameters.class, "measure", new StringParam(MEASURE_URI))
        .andSearchParameter("periodStart", new DateParam("1900"))
        .andSearchParameter("periodEnd", new DateParam("2100"))
        .useHttpGet())
        .thenReturn(operation);
    when(operation.preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class)))
        .thenReturn(operation);
    when(operation
        .execute()).thenReturn(paramerters);

    assertThatThrownBy(() -> fhirConnector.evaluateMeasure(MEASURE_URI))
        .isInstanceOf(IOException.class);
    assertThat(output.getOut()).contains("Response contains unexpected resource type");

  }

  @Test
  @DisplayName("evaluating measure returns Bundle with unknown resource type entry")
  void testEvaluateMeasureFailsWithInvalidResourceType(CapturedOutput output) throws Exception {
    var resource = new CapabilityStatement()
        .setSoftware(new CapabilityStatementSoftwareComponent(new StringType("name-073450")));
    var paramerters = new Parameters()
        .addParameter(new ParametersParameterComponent()
            .setResource(resource));

    when(client.operation()
        .onType(Measure.class)
        .named("evaluate-measure")
        .withSearchParameter(Parameters.class, "measure", new StringParam(MEASURE_URI))
        .andSearchParameter("periodStart", new DateParam("1900"))
        .andSearchParameter("periodEnd", new DateParam("2100"))
        .useHttpGet())
        .thenReturn(operation);
    when(operation.preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class)))
        .thenReturn(operation);
    when(operation
        .execute()).thenReturn(paramerters);

    assertThatThrownBy(() -> fhirConnector.evaluateMeasure(MEASURE_URI))
        .isInstanceOf(IOException.class);
    assertThat(output.getOut()).contains("unexpected resource type");

  }

  @Test
  void testEvaluateMeasureIOException() {
    BaseServerResponseException e = BaseServerResponseException.newInstance(200, "test");
    Mockito.when(client.operation()
            .onType(Measure.class)
            .named("evaluate-measure")
            .withSearchParameter(Parameters.class, "measure", new StringParam(MEASURE_URI))
            .andSearchParameter("periodStart", new DateParam("1900"))
            .andSearchParameter("periodEnd", new DateParam("2100"))
            .useHttpGet())
        .thenReturn(operation);
    when(operation.preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class)))
        .thenReturn(operation);
    when(operation
        .execute()).thenThrow(e);

    assertThatThrownBy(() -> fhirConnector.evaluateMeasure(MEASURE_URI))
        .isInstanceOf(IOException.class);
  }

}
