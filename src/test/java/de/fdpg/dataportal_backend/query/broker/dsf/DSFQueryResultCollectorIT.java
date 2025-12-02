package de.fdpg.dataportal_backend.query.broker.dsf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.fdpg.dataportal_backend.query.collect.QueryStatus;
import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.client.WebsocketClient;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus.COMPLETE;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUMMARY;
import static org.hl7.fhir.r4.model.Task.TaskIntent.ORDER;
import static org.hl7.fhir.r4.model.Task.TaskStatus.COMPLETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NewClassNamingConvention")
public class DSFQueryResultCollectorIT {

  private static final String SINGLE_DIC_RESULT_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/feasibility-task-single-dic-result|1.0";

  @Mock
  private DSFBrokerClient brokerClient;

  @Mock
  private FhirWebserviceClient fhirClient;

  @Mock
  private FhirWebClientProvider fhirWebClientProvider;

  private WebsocketClientMock websocketClient;
  private QueryResultCollector resultCollector;

  @BeforeEach
  public void setUp() {
    FhirContext fhirCtx = FhirContext.forR4();
    DSFQueryResultStore resultStore = new DSFQueryResultStore();
    DSFQueryResultHandler resultHandler = new DSFQueryResultHandler(fhirWebClientProvider);

    websocketClient = new WebsocketClientMock();
    resultCollector = new DSFQueryResultCollector(resultStore, fhirCtx, fhirWebClientProvider, resultHandler);
  }

  private Task createTestTask(String brokerQueryId, String siteId, String measureReportReference, String profile) {
    Task task = new Task()
        .setStatus(COMPLETED)
        .setIntent(ORDER)
        .setAuthoredOn(new Date())
        .setInstantiatesCanonical("http://dsf.dev/bpe/Process/feasibilityRequest|1.0");

    task.getRequester()
        .setType("Organization")
        .getIdentifier().setSystem("http://dsf.dev/fhir/NamingSystem/organization-identifier").setValue(siteId);

    task.getRestriction().getRecipientFirstRep()
        .setType("Organization")
        .getIdentifier().setSystem("http://dsf.dev/fhir/NamingSystem/organization-identifier").setValue("ZARS");

    task.addInput()
        .setType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
                .setCode("message-name")))
        .setValue(new StringType("feasibilityRequestMessage"));
    task.addInput()
        .setType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
                .setCode("business-key")))
        .setValue(new StringType(brokerQueryId));
    task.addInput()
        .setType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://medizininformatik-initiative.de/fhir/CodeSystem/feasibility")
                .setCode("measure-reference")))
        .setValue(new Reference()
            .setReference("urn:uuid:" + UUID.randomUUID()));
    task.addOutput()
        .setType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://medizininformatik-initiative.de/fhir/CodeSystem/feasibility")
                .setCode("measure-report-reference")))
        .setValue(new Reference()
            .setReference(measureReportReference));

    task.setMeta(new Meta().addProfile(profile));

    return task;
  }

  private MeasureReport createTestMeasureReport(int measureCount) {
    MeasureReport measureReport = new MeasureReport()
        .setStatus(COMPLETE)
        .setType(SUMMARY)
        .setDate(new Date());

    measureReport.addGroup()
        .addPopulation()
        .setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/measure-population")
                .setCode("initial-population")))
        .setCount(measureCount);
    return measureReport;
  }

  @Test
  public void testRegisteredListenerGetsNotifiedOnUpdate() throws IOException, FhirWebClientProvisionException {
    String brokerQueryId = UUID.randomUUID().toString();
    String siteId = "DIC";
    String measureReportId = UUID.randomUUID().toString();
    Task task = createTestTask(brokerQueryId, siteId, "MeasureReport/" + measureReportId, SINGLE_DIC_RESULT_PROFILE);

    int measureCount = 5;
    MeasureReport measureReport = createTestMeasureReport(measureCount);

    when(fhirWebClientProvider.provideFhirWebsocketClient()).thenReturn(websocketClient);
    when(fhirWebClientProvider.provideFhirWebserviceClient()).thenReturn(fhirClient);
    when(fhirClient.read(MeasureReport.class, measureReportId)).thenReturn(measureReport);

    var actual = new Object() {
      String brokerQueryId = null;
      String siteId = null;
      QueryStatus status = null;
    };
    resultCollector.addResultListener(brokerClient, (backendQueryId, statusUpdate) -> {
      actual.brokerQueryId = statusUpdate.brokerQueryId();
      actual.siteId = statusUpdate.brokerSiteId();
      actual.status = statusUpdate.status();
    });

    websocketClient.fakeIncomingMessage(task);

    assertEquals(brokerQueryId, actual.brokerQueryId);
    assertEquals(siteId, actual.siteId);
    assertEquals(QueryStatus.COMPLETED, actual.status);
  }

  @Test
  public void testResultFeasibilityIsPresentAfterListenerGetsNotifiedOnUpdate() throws IOException, FhirWebClientProvisionException {
    String measureReportId = UUID.randomUUID().toString();
    Task task = createTestTask(UUID.randomUUID().toString(), "DIC", "MeasureReport/" + measureReportId, SINGLE_DIC_RESULT_PROFILE);

    int measureCount = 5;
    MeasureReport measureReport = createTestMeasureReport(measureCount);

    when(fhirWebClientProvider.provideFhirWebsocketClient()).thenReturn(websocketClient);
    when(fhirWebClientProvider.provideFhirWebserviceClient()).thenReturn(fhirClient);
    when(fhirClient.read(MeasureReport.class, measureReportId)).thenReturn(measureReport);

    resultCollector.addResultListener(brokerClient, (backendQueryId, statusUpdate) -> {
      try {
        int resultFeasibility = resultCollector.getResultFeasibility(statusUpdate.brokerQueryId(),
            statusUpdate.brokerSiteId());

        assertEquals(measureCount, resultFeasibility);
      } catch (Exception e) {
        fail();
      }
    });

    websocketClient.fakeIncomingMessage(task);
  }

  @Test
  public void testSiteIdsArePresentAfterListenerGetsNotifiedOnUpdate() throws IOException, FhirWebClientProvisionException {
    String siteId = "DIC";
    String measureReportId = UUID.randomUUID().toString();
    Task task = createTestTask(UUID.randomUUID().toString(), siteId, "MeasureReport/" + measureReportId, SINGLE_DIC_RESULT_PROFILE);

    int measureCount = 5;
    MeasureReport measureReport = createTestMeasureReport(measureCount);

    when(fhirWebClientProvider.provideFhirWebsocketClient()).thenReturn(websocketClient);
    when(fhirWebClientProvider.provideFhirWebserviceClient()).thenReturn(fhirClient);
    when(fhirClient.read(MeasureReport.class, measureReportId)).thenReturn(measureReport);

    resultCollector.addResultListener(brokerClient, (backendQueryId, statusUpdate) -> {
      try {
        List<String> siteIds = resultCollector.getResultSiteIds(statusUpdate.brokerQueryId());

        assertEquals(List.of(siteId), siteIds);
      } catch (Exception e) {
        fail();
      }
    });

    websocketClient.fakeIncomingMessage(task);
  }

  @Test
  public void testRegisteredListenersGetNotNotifiedOnIncomingTasksThatAreNoResults() throws IOException, FhirWebClientProvisionException {
    String measureReportId = UUID.randomUUID().toString();
    Task task = createTestTask(UUID.randomUUID().toString(), "DIC", "MeasureReport/" + measureReportId, "other-profile");

    when(fhirWebClientProvider.provideFhirWebsocketClient()).thenReturn(websocketClient);
    resultCollector.addResultListener(brokerClient, (backendQueryId, statusUpdate) -> fail());

    websocketClient.fakeIncomingMessage(task);
  }

  private static class WebsocketClientMock implements WebsocketClient {

    private Consumer<Resource> consumer;

    @Override
    public void connect() {
      // DO NOTHING
    }

    @Override
    public void disconnect() {
      // DO NOTHING
    }

    @Override
    public void setResourceHandler(Consumer<Resource> consumer, Supplier<IParser> supplier) {
      this.consumer = consumer;
    }

    @Override
    public void setPingHandler(Consumer<String> consumer) {
      // DO NOTHING
    }

    public void fakeIncomingMessage(DomainResource resource) {
      consumer.accept(resource);
    }
  }
}
