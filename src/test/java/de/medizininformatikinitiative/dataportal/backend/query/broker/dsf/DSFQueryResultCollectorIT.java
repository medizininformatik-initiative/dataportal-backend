package de.medizininformatikinitiative.dataportal.backend.query.broker.dsf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatus;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.PemIo;
import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.client.WebsocketClient;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.Testcontainers;
import org.testcontainers.toxiproxy.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus.COMPLETE;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUMMARY;
import static org.hl7.fhir.r4.model.Task.TaskIntent.ORDER;
import static org.hl7.fhir.r4.model.Task.TaskStatus.COMPLETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
@SuppressWarnings("NewClassNamingConvention")
public class DSFQueryResultCollectorIT {

  private static final String SINGLE_DIC_RESULT_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/feasibility-task-single-dic-result|1.0";
  private static final Logger logger = LoggerFactory.getLogger(DSFQueryResultCollectorIT.class);

  @Mock private DSFBrokerClient brokerClient;

  @Nested
  class ListenerTests {

    @Mock private FhirWebserviceClient fhirClient;
    @Mock private FhirWebClientProvider fhirWebClientProvider;

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

    @Test
    public void testRegisteredListenerGetsNotifiedOnUpdate() throws IOException, FhirWebClientProvisionException {
      String brokerQueryId = UUID.randomUUID().toString();
      String siteId = "DIC";
      String measureReportId = UUID.randomUUID().toString();
      Task task = createTestTask(brokerQueryId, siteId, "MeasureReport/" + measureReportId, SINGLE_DIC_RESULT_PROFILE);

      int measureCount = 5;
      MeasureReport measureReport = createTestMeasureReport(measureCount);

      when(fhirWebClientProvider.provideFhirWebsocketClient(any(Runnable.class))).thenReturn(websocketClient);
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
    public void testResultFeasibilityIsPresentAfterListenerGetsNotifiedOnUpdate()
        throws IOException, FhirWebClientProvisionException {
      String measureReportId = UUID.randomUUID().toString();
      Task task = createTestTask(UUID.randomUUID().toString(), "DIC", "MeasureReport/" + measureReportId,
          SINGLE_DIC_RESULT_PROFILE);

      int measureCount = 5;
      MeasureReport measureReport = createTestMeasureReport(measureCount);

      when(fhirWebClientProvider.provideFhirWebsocketClient(any(Runnable.class))).thenReturn(websocketClient);
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
    public void testSiteIdsArePresentAfterListenerGetsNotifiedOnUpdate()
        throws IOException, FhirWebClientProvisionException {
      String siteId = "DIC";
      String measureReportId = UUID.randomUUID().toString();
      Task task = createTestTask(UUID.randomUUID().toString(), siteId, "MeasureReport/" + measureReportId,
          SINGLE_DIC_RESULT_PROFILE);

      int measureCount = 5;
      MeasureReport measureReport = createTestMeasureReport(measureCount);

      when(fhirWebClientProvider.provideFhirWebsocketClient(any(Runnable.class))).thenReturn(websocketClient);
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
    public void testRegisteredListenersGetNotNotifiedOnIncomingTasksThatAreNoResults()
        throws IOException, FhirWebClientProvisionException {
      String measureReportId = UUID.randomUUID().toString();
      Task task = createTestTask(UUID.randomUUID().toString(), "DIC", "MeasureReport/" + measureReportId,
          "other-profile");

      when(fhirWebClientProvider.provideFhirWebsocketClient(any(Runnable.class))).thenReturn(websocketClient);
      resultCollector.addResultListener(brokerClient, (backendQueryId, statusUpdate) -> fail());

      websocketClient.fakeIncomingMessage(task);
    }
  }

  @Nested
  class ReconnectorTests {
    private static final Pattern RECOVERED_PATTERN = Pattern.compile("Websocket connection recovered");
    private static final int MEASURE_REPORT_COUNT = 130501;
    private static final String SUBSCRIPTION_ID = "862d263e-5715-4643-b961-1949d8f251ba";
    private static final String MEASURE_RPORT_ID = "a31daf36-d93a-4a2c-ae5f-2c11b7f3cc8c";
    private static final Random RANDOM_NUMBER_GENERATOR = new Random();

    private static File clientCertificateFile;
    private static File serverCertificateFile;
    private static File privateKeyFile;
    private static KeyPair clientKeyPair;
    private static HeldCertificate rootCertificate;
    private static HeldCertificate clientCertificate;
    private static HeldCertificate serverCertificate;
    private static HandshakeCertificates serverCertificates;

    private ToxiproxyContainer toxiProxy = new ToxiproxyContainer(DockerImageName.parse("shopify/toxiproxy:2.1.4"));
    private FhirContext fhirContext = FhirContext.forR4();
    private IParser jsonParser = fhirContext.newJsonParser();
    private TestWebSocketListener listener;
    private MockWebServer server;
    private Proxy proxy;
    private DSFQueryResultCollector resultCollector;

    @BeforeAll
    static void init() throws Exception {
      clientKeyPair = CertificateHelper.createRsaKeyPair4096Bit();
      rootCertificate = new HeldCertificate.Builder().certificateAuthority(0).build();
      clientCertificate = new HeldCertificate.Builder().keyPair(clientKeyPair).signedBy(rootCertificate).build();
      serverCertificate = new HeldCertificate.Builder().commonName("ingen")
          .addSubjectAlternativeName("localhost")
          .signedBy(rootCertificate)
          .build();
      serverCertificates = new HandshakeCertificates.Builder()
          .addTrustedCertificate(rootCertificate.certificate())
          .heldCertificate(serverCertificate)
          .build();
      privateKeyFile = createPrivateKeyFile(clientKeyPair);
      clientCertificateFile = createCertificateFile(clientCertificate);
      serverCertificateFile = createCertificateFile(serverCertificate);
    }

    @BeforeEach
    void setup() throws Exception {
      listener = new TestWebSocketListener();
      server = createMockServer();

      var port = server.getPort();
      Testcontainers.exposeHostPorts(port);
      toxiProxy.start();
      var toxiproxyClient = new ToxiproxyClient(toxiProxy.getHost(), toxiProxy.getControlPort());
      proxy = toxiproxyClient.createProxy("fhir-proxy", "0.0.0.0:8666", "host.testcontainers.internal:" + port);

      var webserviceBaseUrl = format("https://%s:%s/fhir", toxiProxy.getHost(), toxiProxy.getMappedPort(8666));
      var websocketBaseUrl = format("wss://%s:%s/fhir/ws", toxiProxy.getHost(), toxiProxy.getMappedPort(8666));
      var securityContextProvider = new DSFFhirSecurityContextProvider(
          clientCertificateFile.getPath(), privateKeyFile.getPath(), "password", serverCertificateFile.getPath());
      var proxyContext = new FhirProxyContext(null, null, null);
      var clientProvider = new DSFFhirWebClientProvider(fhirContext, webserviceBaseUrl,
          40000, 2000, websocketBaseUrl, securityContextProvider, proxyContext, false);
      var resultStore = new DSFQueryResultStore();
      var resultHandler = new DSFQueryResultHandler(clientProvider);
      resultCollector = new DSFQueryResultCollector(resultStore, fhirContext, clientProvider, resultHandler);
    }

    @AfterEach
    void shutdown() throws Exception {
      toxiProxy.stop();
      server.shutdown();
      resultCollector.close();
    }

    private MockWebServer createMockServer() throws IOException {
      var mockWeb = new MockWebServer();
      mockWeb.useHttps(serverCertificates.sslSocketFactory(), false);
      mockWeb.setDispatcher(createDispatcher());
      return mockWeb;
    }

    private Dispatcher createDispatcher() {
      return new Dispatcher() {

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
          var subscription = createTestSubscription(SUBSCRIPTION_ID);
          var subscriptionJson = jsonParser.encodeResourceToString(subscription);
          var subscriptionSearchBundleJson = jsonParser
              .encodeResourceToString(wrapResourcesInSearchsetBundle(List.of(subscription)));
          var measureReportJson = jsonParser.encodeResourceToString(createTestMeasureReport(MEASURE_REPORT_COUNT));


          if (request.getRequestUrl().scheme().equalsIgnoreCase("https")) {
            if (request.getMethod().equals("GET")) {
              switch (request.getRequestUrl().encodedPath()) {
                case "/fhir/MeasureReport/" + MEASURE_RPORT_ID:
                  return new MockResponse()
                      .setResponseCode(200)
                      .setHeader("Content-Type", "application/fhir+json")
                      .setBody(measureReportJson);
                case "/fhir/Subscription/" + SUBSCRIPTION_ID:
                  return new MockResponse()
                      .setResponseCode(200)
                      .setHeader("Content-Type", "application/fhir+json")
                      .setBody(subscriptionJson);
                case "/fhir/Subscription":
                  return new MockResponse()
                      .setResponseCode(200)
                      .setHeader("Content-Type", "application/fhir+json")
                      .setBody(subscriptionSearchBundleJson);
                case "/fhir/ws":
                  return new MockResponse()
                      .withWebSocketUpgrade(listener)
                      .setResponseCode(101);
              }
            }

            if (request.getMethod().equals("DELETE")) {
              switch (request.getRequestUrl().encodedPath()) {
                case "/fhir/MeasureReport/" + MEASURE_RPORT_ID:
                  return new MockResponse()
                      .setResponseCode(200)
                      .setHeader("Content-Type", "application/fhir+json")
                      .setBody(measureReportJson);
              }
            }

            if (request.getMethod().equals("POST")) {
              switch (request.getRequestUrl().encodedPath()) {
                case "/fhir/Subscription":
                  return new MockResponse()
                      .setResponseCode(201)
                      .setHeader("Content-Type", "application/fhir+json")
                      .setHeader("Location", "/fhir/Subscription/862d263e-5715-4643-b961-1949d8f251ba")
                      .setBody(subscriptionJson);
                case "/fhir/MeasureReport/" + MEASURE_RPORT_ID + "/$permanent-delete":
                  return new MockResponse()
                      .setResponseCode(200)
                      .setHeader("Content-Type", "application/fhir+json")
                      .setBody(measureReportJson);
              }
            }
          }
          return new MockResponse()
              .setResponseCode(404);
        }
      };
    }

    private static File createPrivateKeyFile(KeyPair pair)
        throws IOException, OperatorCreationException {
      var tempFile = Files.createTempFile("privKey", ".pem").toFile();
      PemIo.writeNotEncryptedPrivateKeyToOpenSslClassicPem(new BouncyCastleProvider(), tempFile.toPath(),
          pair.getPrivate());
      tempFile.deleteOnExit();
      return tempFile;
    }

    private static File createCertificateFile(HeldCertificate certificate) throws IOException {
      var tempFile = Files.createTempFile("cert", ".pem").toFile();
      try (var fos = new FileOutputStream(tempFile)) {
        fos.write(certificate.certificatePem().getBytes());
      }
      tempFile.deleteOnExit();
      return tempFile;
    }

    @Test
    @DisplayName("Websocket connects and task resource sent over websocket is handled by result handler")
    void receivesTask(CapturedOutput output) throws Exception {
      resultCollector.addResultListener(brokerClient, (backendQueryId, statusUpdate) -> {
        logger.info("BackendQuery Status {}", statusUpdate.status().name());
      });

      await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
        assertThat(output.toString()).contains("Websocket open");
        assertThat(listener.getOpenCount()).isEqualTo(1);
      });
      var ws = assertThat(listener.getWebSocket()).isPresent().actual().get();
      // signal to client that it is bound to subscription
      ws.send("bound " + SUBSCRIPTION_ID);
      // send completed task resource with reference to measure report to client
      ws.send(jsonParser.encodeResourceToString(createTestTask(UUID.randomUUID().toString(), "DIC",
          "MeasureReport/" + MEASURE_RPORT_ID, SINGLE_DIC_RESULT_PROFILE)));

      // wait for result handler to be called
      await().atMost(30, TimeUnit.SECONDS)
          .untilAsserted(() -> assertThat(output.toString()).contains("BackendQuery Status COMPLETED"));
      // check result handler is called only once
      await().during(5, TimeUnit.SECONDS)
          .untilAsserted(() -> assertThat(output.toString()).containsOnlyOnce("BackendQuery Status COMPLETED"));
    }

    @Test
    @DisplayName("Websocket get reconnected after connection loss and result handler triggers only once")
    void singleReconnect(CapturedOutput output) throws Exception {
      resultCollector.addResultListener(brokerClient, (backendQueryId, statusUpdate) -> {
        logger.info("BackendQuery Status {}", statusUpdate.status().name());
      });

      await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
        assertThat(output.toString()).contains("Websocket open");
        assertThat(listener.getOpenCount()).isEqualTo(1);
      });

      // simulate disconnect
      proxy.toxics().timeout("RESET_CONNECTION_UPSTREAM", ToxicDirection.UPSTREAM, 1000);
      await().atMost(30, TimeUnit.SECONDS)
          .untilAsserted(() -> assertThat(output.toString()).contains("Trying to reconnect websocket"));

      Thread.sleep(RANDOM_NUMBER_GENERATOR.nextLong(500, 5000));

      proxy.toxics().get("RESET_CONNECTION_UPSTREAM").remove();
      await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
        assertThat(output.toString()).containsOnlyOnce("Websocket connection recovered");
        assertThat(listener.getOpenCount()).isEqualTo(2);
      });

      var ws = assertThat(listener.getWebSocket()).isPresent().actual().get();
      // signal to client that it is bound to subscription
      ws.send("bound " + SUBSCRIPTION_ID);
      // send completed task resource with reference to measure report to client
      ws.send(jsonParser.encodeResourceToString(createTestTask(UUID.randomUUID().toString(), "DIC",
          "MeasureReport/" + MEASURE_RPORT_ID, SINGLE_DIC_RESULT_PROFILE)));

      // wait for result handler to be called
      await().atMost(10, TimeUnit.SECONDS)
          .untilAsserted(() -> assertThat(output.toString()).contains("BackendQuery Status COMPLETED"));
      // check result handler is called only once
      await().during(5, TimeUnit.SECONDS)
          .untilAsserted(() -> assertThat(output.toString()).containsOnlyOnce("BackendQuery Status COMPLETED"));
    }

    @Test
    @DisplayName("Websocket client gets reconnected multiple times but result handler triggers only once")
    void multipleReconnects(CapturedOutput output) throws Exception {
      resultCollector.addResultListener(brokerClient, (backendQueryId, statusUpdate) -> {
        logger.info("BackendQuery Status {}", statusUpdate.status().name());
      });

      await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
        assertThat(output.toString()).contains("Websocket open");
        assertThat(listener.getOpenCount()).isEqualTo(1);
      });

      // simulate 3 disconnects
      for (int i = 0; i < 3; i++) {
        final int currentIteration = i;

        proxy.toxics().timeout("RESET_CONNECTION_UPSTREAM", ToxicDirection.UPSTREAM, 1000);
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(output.toString()).contains("Trying to reconnect websocket"));

        Thread.sleep(RANDOM_NUMBER_GENERATOR.nextLong(500, 5000));

        proxy.toxics().get("RESET_CONNECTION_UPSTREAM").remove();
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
          assertThat(RECOVERED_PATTERN.matcher(output.toString()).results().count())
              .isEqualTo(currentIteration + 1);
          assertThat(listener.getOpenCount()).isEqualTo(currentIteration + 2);
        });
      }
      var ws = assertThat(listener.getWebSocket()).isPresent().actual().get();
      // signal to client that it is bound to subscription
      ws.send("bound " + SUBSCRIPTION_ID);
      // send completed task resource with reference to measure report to client
      ws.send(jsonParser.encodeResourceToString(createTestTask(UUID.randomUUID().toString(), "DIC",
          "MeasureReport/" + MEASURE_RPORT_ID, SINGLE_DIC_RESULT_PROFILE)));

      // wait for result handler to be called
      await().atMost(10, TimeUnit.SECONDS)
          .untilAsserted(() -> assertThat(output.toString()).contains("BackendQuery Status COMPLETED"));
      // check result handler is called only once
      await().during(5, TimeUnit.SECONDS)
          .untilAsserted(() -> assertThat(output.toString()).containsOnlyOnce("BackendQuery Status COMPLETED"));
    }
  }

  private Task createTestTask(String brokerQueryId, String siteId, String measureReportReference,
                                     String profile) {
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

  private Subscription createTestSubscription(String id) {
    return (Subscription) new Subscription()
        .setCriteria("Task?staus=completed")
        .setChannel(new SubscriptionChannelComponent().setType(SubscriptionChannelType.WEBSOCKET)
            .setPayload("application/fhir+json"))
        .setReason("Waiting for query results")
        .setStatus(SubscriptionStatus.ACTIVE).setId(id);
  }

  private Bundle wrapResourcesInSearchsetBundle(List<Resource> resources) {
    return new Bundle().setType(BundleType.SEARCHSET).setTotal(resources.size())
        .setEntry(resources.stream().map(r -> new Bundle.BundleEntryComponent().setResource(r)).toList());
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

  private static class TestWebSocketListener extends WebSocketListener {
    private AtomicReference<WebSocket> webSocket = new AtomicReference<WebSocket>();
    private AtomicInteger openCount = new AtomicInteger(0);

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
      this.webSocket.set(webSocket);
      openCount.incrementAndGet();
      super.onOpen(webSocket, response);
    }

    public Optional<WebSocket> getWebSocket() {
      return Optional.ofNullable(webSocket.get());
    }

    public int getOpenCount() {
      return openCount.get();
    }
  }
}
