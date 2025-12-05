package de.medizininformatikinitiative.dataportal.backend.query.broker.dsf;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.client.FhirWebserviceClientJersey;
import dev.dsf.fhir.client.WebsocketClient;
import dev.dsf.fhir.client.WebsocketClientTyrus;
import dev.dsf.fhir.service.ReferenceCleanerImpl;
import dev.dsf.fhir.service.ReferenceExtractorImpl;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType.WEBSOCKET;
import static org.hl7.fhir.r4.model.Subscription.SubscriptionStatus.ACTIVE;
import static org.hl7.fhir.r4.model.Task.TaskStatus.COMPLETED;

/**
 * An entity that can provide different kinds of web clients to communicate with a FHIR server.
 */
class DSFFhirWebClientProvider implements FhirWebClientProvider {

  private static final Logger logger = LoggerFactory.getLogger(DSFFhirWebClientProvider.class);

  private static final String QUERY_RESULT_SUBSCRIPTION_REASON = "Waiting for query results";
  private static final String QUERY_RESULT_SUBSCRIPTION_CHANNEL_PAYLOAD = "application/fhir+json";
  private static final String CODE_SYSTEM_READ_ACCESS_TAG = "http://dsf.dev/fhir/CodeSystem/read-access-tag";
  private static final String CODE_SYSTEM_READ_ACCESS_TAG_VALUE_ALL = "ALL";

  private final FhirContext fhirContext;
  private final String webserviceBaseUrl;
  private final int webserviceReadTimeout;
  private final int webserviceConnectTimeout;
  private final String websocketUrl;
  private final FhirSecurityContextProvider securityContextProvider;
  private final FhirProxyContext proxyContext;
  private FhirSecurityContext securityContext;
  private boolean logRequests;


  public DSFFhirWebClientProvider(FhirContext fhirContext, String webserviceBaseUrl, int webserviceReadTimeout,
                                  int webserviceConnectTimeout, String websocketUrl,
                                  FhirSecurityContextProvider securityContextProvider,
                                  FhirProxyContext proxyContext,
                                  boolean logRequests) {
    this.fhirContext = fhirContext;
    this.webserviceBaseUrl = webserviceBaseUrl;
    this.webserviceReadTimeout = webserviceReadTimeout;
    this.webserviceConnectTimeout = webserviceConnectTimeout;
    this.websocketUrl = websocketUrl;
    this.securityContextProvider = securityContextProvider;
    this.proxyContext = proxyContext;
    this.logRequests = logRequests;
  }

  /**
   * Package-visible helper that constructs the reconnector Runnable. Extracted for easier testing.
   *
   * @param wsRef an AtomicReference holding the WebsocketClient instance used for reconnect attempts
   * @return a Runnable that attempts reconnection
   */
  static Runnable createReconnector(AtomicReference<WebsocketClient> wsRef) {
    return () -> {
      var attempt = 0;
      while (true) {
        var c = wsRef.get();
        if (c == null) {
          logger.error("Expected websocket client being set but got null.");
          return;
        }

        try {
          logger.info("Websocket connection recovery attempt #{} ...", attempt);
          c.connect();
          logger.info("Websocket connection recovered");
          return;
        } catch (Exception e) {
          attempt++;
          logger.error("Websocket connection recovery attempt #{} failed: {}", attempt, e.getMessage());
        }
      }
    };
  }

  @Override
  public FhirWebserviceClient provideFhirWebserviceClient() throws FhirWebClientProvisionException {
    if (securityContext == null) {
      try {
        securityContext = securityContextProvider.provideSecurityContext();
      } catch (FhirSecurityContextProvisionException e) {
        throw new FhirWebClientProvisionException(e);
      }
    }

    ReferenceExtractorImpl extractor = new ReferenceExtractorImpl();
    ReferenceCleanerImpl cleaner = new ReferenceCleanerImpl(extractor);

    return new FhirWebserviceClientJersey(
        webserviceBaseUrl,
        securityContext.getTrustStore(),
        securityContext.getKeyStore(),
        securityContext.getKeyStorePassword(),
        null,
        proxyContext.getProxyHost(),
        proxyContext.getUsername(),
        proxyContext.getPassword(),
        webserviceConnectTimeout,
        webserviceReadTimeout,
        logRequests,
        null,
        fhirContext,
        cleaner);
  }

  @Override
  public WebsocketClient provideFhirWebsocketClient() throws FhirWebClientProvisionException {
    if (securityContext == null) {
      try {
        securityContext = securityContextProvider.provideSecurityContext();
      } catch (FhirSecurityContextProvisionException e) {
        throw new FhirWebClientProvisionException(e);
      }
    }

    var fhirClient = provideFhirWebserviceClient();

    var subscriptionId = searchForExistingQueryResultSubscription(fhirClient)
        .orElseGet(createQueryResultSubscription(fhirClient))
        .getIdElement().getIdPart();

    var wsRef = new AtomicReference<WebsocketClient>();
    var client = new WebsocketClientTyrus(createReconnector(wsRef),
        URI.create(websocketUrl),
        securityContext.getTrustStore(),
        securityContext.getKeyStore(),
        securityContext.getKeyStorePassword(),
        proxyContext.getProxyHost(),
        proxyContext.getUsername(),
        proxyContext.getPassword(),
        null, subscriptionId);
    wsRef.set(client);
    return client;
  }

  /**
   * Searches for an existing feasibility query result subscription and returns it if there is any.
   *
   * @return The subscription for query results.
   */
  private Optional<Subscription> searchForExistingQueryResultSubscription(FhirWebserviceClient fhirClient) {
    Bundle bundle = fhirClient.searchWithStrictHandling(Subscription.class,
        Map.of("criteria", Collections.singletonList("Task?status=" + COMPLETED.toCode()),
            "status", Collections.singletonList(ACTIVE.toCode()),
            "type", Collections.singletonList(WEBSOCKET.toCode()),
            "payload", Collections.singletonList(QUERY_RESULT_SUBSCRIPTION_CHANNEL_PAYLOAD)));

    if (!Bundle.BundleType.SEARCHSET.equals(bundle.getType()))
      throw new RuntimeException("Could not retrieve searchset for subscription search query, but got "
          + bundle.getType());
    if (bundle.getTotal() == 0)
      return Optional.empty();
    if (bundle.getTotal() != 1)
      throw new RuntimeException("Could not retrieve exactly one result for subscription search query");
    if (!(bundle.getEntryFirstRep().getResource() instanceof Subscription))
      throw new RuntimeException("Could not retrieve exactly one Subscription, but got "
          + bundle.getEntryFirstRep().getResource().getResourceType());

    return Optional.of((Subscription) bundle.getEntryFirstRep().getResource());
  }

  /**
   * Returns a function capable of supplying a newly created subscription for feasibility query results.
   *
   * @return Function for getting a subscription for feasibility query results.
   */
  private Supplier<Subscription> createQueryResultSubscription(FhirWebserviceClient fhirClient) {
    return () -> {
      Subscription subscription = new Subscription()
          .setStatus(ACTIVE)
          .setReason(QUERY_RESULT_SUBSCRIPTION_REASON)
          .setChannel(new Subscription.SubscriptionChannelComponent()
              .setType(WEBSOCKET)
              .setPayload(QUERY_RESULT_SUBSCRIPTION_CHANNEL_PAYLOAD))
          // TODO: use this criteria if DSF has implemented the _profile search parameter (make sure to also remove the profile check in the DSFQueryResultHandler class!)
//                .setCriteria("Task?status=" + Task.TaskStatus.COMPLETED.toCode() + "&_profile=" + SINGLE_DIC_QUERY_RESULT_PROFILE);
          .setCriteria("Task?status=" + COMPLETED.toCode());

      subscription.getMeta()
          .addTag()
          .setSystem(CODE_SYSTEM_READ_ACCESS_TAG)
          .setCode(CODE_SYSTEM_READ_ACCESS_TAG_VALUE_ALL);

      return fhirClient.create(subscription);
    };
  }
}
