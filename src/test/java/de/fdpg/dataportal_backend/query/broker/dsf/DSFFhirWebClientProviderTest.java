package de.fdpg.dataportal_backend.query.broker.dsf;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.client.WebsocketClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith({OutputCaptureExtension.class, MockitoExtension.class})
public class DSFFhirWebClientProviderTest {

  private static final String CAPABILITY_STATEMENT = """
      {
        "date": "2025-10-21T19:42:39+02:00",
        "fhirVersion": "4.0.1",
        "format": [
          "application/fhir+json"
        ],
        "kind": "instance",
        "resourceType": "CapabilityStatement",
        "software": {
          "name": "Test",
          "version": "0.0.0"
        },
        "status": "active"
      }
      """;

  @Mock
  private DSFFhirSecurityContextProvider securityContextProvider;

  private MockWebServer mockWeb;
  private DSFFhirWebClientProvider clientProvider;

  @BeforeEach
  void setUp() throws Exception {
    mockWeb = new MockWebServer();
    mockWeb.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWeb.shutdown();
  }

  private DSFFhirWebClientProvider createClientProvider(boolean logRequests) {
    String webserviceBaseUrl = format("http://%s:%s/fhir", mockWeb.getHostName(), mockWeb.getPort());
    String websocketBaseUrl = format("ws://%s:%s/fhir/ws", mockWeb.getHostName(), mockWeb.getPort());
    FhirProxyContext proxyContext = new FhirProxyContext(null, null, null);
    DSFFhirWebClientProvider clientProvider = new DSFFhirWebClientProvider(FhirContext.forR4(), webserviceBaseUrl,
        20000, 2000, websocketBaseUrl, securityContextProvider, proxyContext, logRequests);
    return clientProvider;
  }

  @Nested
  @DisplayName("requests are logged when logRequests flag is set to true")
  class SettingLogRequestsFlagToTrueEnablesRequestLogs {

    @BeforeEach
    void setup() throws Exception {
      clientProvider = createClientProvider(true);
      when(securityContextProvider.provideSecurityContext())
          .thenReturn(new FhirSecurityContext(null, null, null));
      mockWeb.enqueue(new MockResponse().setResponseCode(200)
          .setHeader("Content-Type", "application/fhir+json")
          .setBody(CAPABILITY_STATEMENT));
    }

    @Test
    void run(CapturedOutput output) throws Exception {
      clientProvider.provideFhirWebserviceClient().getConformance();

      assertThat(output)
          .containsIgnoringCase("sending client request")
          .containsIgnoringCase("client response received");
    }
  }

  @Nested
  @DisplayName("requests are not logged when logRequests flag is set to false")
  class SettingLogRequestsFlagToFalseDisablesRequestLogs {

    @BeforeEach
    void setup() throws Exception {
      clientProvider = createClientProvider(false);
      when(securityContextProvider.provideSecurityContext())
          .thenReturn(new FhirSecurityContext(null, null, null));
      mockWeb.enqueue(new MockResponse().setResponseCode(200)
          .setHeader("Content-Type", "application/fhir+json")
          .setBody(CAPABILITY_STATEMENT));
    }

    @Test
    void run(CapturedOutput output) throws Exception {
      clientProvider.provideFhirWebserviceClient().getConformance();

      assertThat(output)
          .doesNotContainIgnoringCase("sending client request")
          .doesNotContainIgnoringCase("client response received");
    }
  }

  @Nested
  @DisplayName("errors are wrapped when providing a client fails")
  class ProvdingClientFailureIsWrapped {

    @BeforeEach
    void setup() throws Exception {
      clientProvider = createClientProvider(false);
      when(securityContextProvider.provideSecurityContext())
          .thenThrow(new FhirSecurityContextProvisionException("foo"));
    }

    @Test
    @DisplayName("providing WebserviceClient fails")
    void failsProvidingWebserviceClient() throws Exception {
      assertThatThrownBy(() -> clientProvider.provideFhirWebserviceClient())
          .isInstanceOf(FhirWebClientProvisionException.class)
          .cause().isInstanceOf(FhirSecurityContextProvisionException.class);
    }

    @Test
    @DisplayName("providing WebsocketClient fails")
    void failsProvidingWebsocketClient() throws Exception {
      assertThatThrownBy(() -> clientProvider.provideFhirWebsocketClient())
          .isInstanceOf(FhirWebClientProvisionException.class)
          .cause().isInstanceOf(FhirSecurityContextProvisionException.class);
    }
  }

  @Nested
  @DisplayName("reconnector function is handling errors")
  class ReconnectorTest {

    @Mock
    WebsocketClient client;
    private AtomicReference<WebsocketClient> wsRef;
    private Runnable reconnector;
    private CountDownLatch latch;
    private AtomicInteger attemptCounter = new AtomicInteger(0);

    @BeforeEach
    void setup() throws Exception {
      wsRef = new AtomicReference<>();
      reconnector = DSFFhirWebClientProvider.createReconnector(wsRef);
      latch = new CountDownLatch(1);
      attemptCounter = new AtomicInteger(0);
    }

    @Test
    @DisplayName("reconnector stops when no websocket client is set")
    void reconnectorReturnsImmediatelyWhenClientIsNull(CapturedOutput output) throws Exception {
      wsRef.set(null);

      // run the reconnector in a background thread which should quickly return because client is null
      ForkJoinPool.commonPool().submit(reconnector);

      await().atMost(5, TimeUnit.SECONDS)
          .untilAsserted(() -> assertThat(output.toString())
              .contains("Expected websocket client being set but got null.")
              .doesNotContain("Websocket connection recovered")
              .doesNotContain("Websocket connection recovery attempt")
          );
    }

    @Test
    @DisplayName("reconnector retries connecting client if client raises Exception on connection attempt")
    void reconnectorRetriesThenSucceeds(CapturedOutput output) throws Exception {
      wsRef.set(client);
      // First call throws, second call succeeds and counts down the latch
      doAnswer(invocation -> {
        int attempt = attemptCounter.incrementAndGet();
        if (attempt == 1) {
          throw new RuntimeException("simulated exceptional connect failure");
        } else {
          latch.countDown();
          return null;
        }
      }).when(client).connect();

      ForkJoinPool.commonPool().submit(reconnector);

      // wait for the mock to report a successful connect
      var success = latch.await(5, TimeUnit.SECONDS);
      assertThat(success).isTrue();
      assertThat(output.toString()).contains("Websocket connection recovered");
    }
  }

}
