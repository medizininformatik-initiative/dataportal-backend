package de.medizininformatikinitiative.dataportal.backend.query.broker.dsf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Subscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith({OutputCaptureExtension.class, MockitoExtension.class})
public class DSFFhirWebClientProviderTest {

  private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
  private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser();

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

  @Mock private DSFFhirSecurityContextProvider securityContextProvider;

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
    DSFFhirWebClientProvider clientProvider = new DSFFhirWebClientProvider(FHIR_CONTEXT, webserviceBaseUrl,
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
  @DisplayName("providing a client")
  class ProvdingClient {

    @BeforeEach
    void setup() throws Exception {
      clientProvider = createClientProvider(false);
    }

    @Test
    @DisplayName("providing WebserviceClient fails")
    void failsProvidingWebserviceClient() throws Exception {
      when(securityContextProvider.provideSecurityContext())
          .thenThrow(new FhirSecurityContextProvisionException("foo"));
      assertThatThrownBy(() -> clientProvider.provideFhirWebserviceClient())
          .isInstanceOf(FhirWebClientProvisionException.class)
          .cause().isInstanceOf(FhirSecurityContextProvisionException.class);
    }

    @Test
    @DisplayName("providing WebsocketClient fails")
    void failsProvidingWebsocketClient() throws Exception {
      when(securityContextProvider.provideSecurityContext())
          .thenThrow(new FhirSecurityContextProvisionException("foo"));
      assertThatThrownBy(() -> clientProvider.provideFhirWebsocketClient(() -> {}))
          .isInstanceOf(FhirWebClientProvisionException.class)
          .cause().isInstanceOf(FhirSecurityContextProvisionException.class);
    }

    @Test
    @DisplayName("providing WebsocketClient fails when search result is no searchset")
    void failsProvidingWebsocketClientWithNonSearchsetBundle() throws Exception {
      when(securityContextProvider.provideSecurityContext()).thenReturn(new FhirSecurityContext(null, null, null));
      mockWeb.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader("Content-Type", "application/fhir+json")
          .setBody(JSON_PARSER.encodeResourceToString(new Bundle().setType(BundleType.BATCH))));
      assertThatThrownBy(() -> clientProvider.provideFhirWebsocketClient(() -> {}))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Could not retrieve searchset for subscription search query, but got BATCH");
    }

    @Test
    @DisplayName("providing WebsocketClient fails when search result contains multiple subscriptions")
    void failsProvidingWebsocketClientWithMultipleSubscriptions() throws Exception {
      when(securityContextProvider.provideSecurityContext()).thenReturn(new FhirSecurityContext(null, null, null));
      mockWeb.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader("Content-Type", "application/fhir+json")
          .setBody(JSON_PARSER.encodeResourceToString(new Bundle().setType(BundleType.SEARCHSET).setTotal(2))));
      assertThatThrownBy(() -> clientProvider.provideFhirWebsocketClient(() -> {}))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Could not retrieve exactly one result for subscription search query");
    }

    @Test
    @DisplayName("providing WebsocketClient fails when search result contains a non-subscription resource")
    void failsProvidingWebsocketClientWithWrongResource() throws Exception {
      when(securityContextProvider.provideSecurityContext()).thenReturn(new FhirSecurityContext(null, null, null));
      mockWeb.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader("Content-Type", "application/fhir+json")
          .setBody(JSON_PARSER.encodeResourceToString(new Bundle().setType(BundleType.SEARCHSET).setTotal(1)
              .addEntry(new BundleEntryComponent().setResource(new MeasureReport().setId("measure-233514"))))));
      assertThatThrownBy(() -> clientProvider.provideFhirWebsocketClient(() -> {}))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Could not retrieve exactly one Subscription, but got MeasureReport");
    }

    @Test
    @DisplayName("providing WebsocketClient succeeds with creating new subscription resource")
    void succeedsProvidingWebsocketClientWithNoSubscription() throws Exception {
      when(securityContextProvider.provideSecurityContext()).thenReturn(new FhirSecurityContext(null, null, null));
      mockWeb.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader("Content-Type", "application/fhir+json")
          .setBody(JSON_PARSER.encodeResourceToString(new Bundle().setType(BundleType.SEARCHSET).setTotal(0))));
      mockWeb.enqueue(new MockResponse().setResponseCode(201).setHeader("Content-Type", "application/fhir+json")
          .setBody(JSON_PARSER.encodeResourceToString(new Subscription().setId("sub-001040"))));

      clientProvider.provideFhirWebsocketClient(() -> {});

      assertThat(mockWeb.getRequestCount()).isEqualTo(2);
      var firstRequest = mockWeb.takeRequest();
      assertThat(firstRequest.getMethod()).isEqualTo("GET");
      assertThat(firstRequest.getPath()).startsWith("/fhir/Subscription");
      var secondRequest = mockWeb.takeRequest();
      assertThat(secondRequest.getMethod()).isEqualTo("POST");
      assertThat(secondRequest.getPath()).isEqualTo("/fhir/Subscription");
    }

    @Test
    @DisplayName("providing WebsocketClient succeeds with existing subscription resource")
    void succeedsProvidingWebsocketClientWithExistingSubscription() throws Exception {
      when(securityContextProvider.provideSecurityContext()).thenReturn(new FhirSecurityContext(null, null, null));
      mockWeb.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader("Content-Type", "application/fhir+json")
          .setBody(JSON_PARSER.encodeResourceToString(new Bundle().setType(BundleType.SEARCHSET).setTotal(1)
              .addEntry(new BundleEntryComponent().setResource(new Subscription().setId("sub-001040"))))));

      clientProvider.provideFhirWebsocketClient(() -> {});

      assertThat(mockWeb.getRequestCount()).isEqualTo(1);
      var request = mockWeb.takeRequest();
      assertThat(request.getMethod()).isEqualTo("GET");
      assertThat(request.getPath()).startsWith("/fhir/Subscription");
    }
  }
}
