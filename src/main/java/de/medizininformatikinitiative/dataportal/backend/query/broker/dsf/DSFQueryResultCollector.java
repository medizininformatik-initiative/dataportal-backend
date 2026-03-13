package de.medizininformatikinitiative.dataportal.backend.query.broker.dsf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.medizininformatikinitiative.dataportal.backend.query.broker.QueryNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.broker.SiteNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatus;
import de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatusListener;
import de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatusUpdate;
import dev.dsf.fhir.client.WebsocketClient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collector for collecting the results of feasibility queries that are running in a distributed fashion.
 * <p>
 * The collector gathers query results from a single FHIR server. Communication with this FHIR server
 * happens using a websocket. The FHIR server sends all task resources that are associated with a subscription.
 */
class DSFQueryResultCollector implements QueryResultCollector, AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(DSFQueryResultCollector.class);
  private static final int MAX_DELAY_MS = 30000;
  private static final int START_DELAY_MS = 500;

  private final Map<DSFBrokerClient, QueryStatusListener> listeners = new ConcurrentHashMap<>();
  private final AtomicBoolean websocketConnectionEstablished = new AtomicBoolean(false);
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private final AtomicInteger delay = new AtomicInteger(START_DELAY_MS);
  private final AtomicBoolean connectingWebsocket = new AtomicBoolean(false);
  private final QueryResultStore store;
  private final FhirContext fhirContext;
  private final FhirWebClientProvider fhirWebClientProvider;
  private final DSFQueryResultHandler resultHandler;

  /**
   * Creates a new {@link DSFQueryResultCollector}.
   *
   * @param store                 Storage facility for storing collected results.
   * @param fhirContext           The FHIR context used for communication purposes with the FHIR server results are
   *                              gathered from.
   * @param fhirWebClientProvider Provider capable of providing a websocket client.
   * @param resultHandler         Handler able to process query results received from the FHIR server.
   */
  public DSFQueryResultCollector(QueryResultStore store, FhirContext fhirContext,
                                 FhirWebClientProvider fhirWebClientProvider, DSFQueryResultHandler resultHandler) {
    this.store = store;
    this.fhirContext = fhirContext;
    this.fhirWebClientProvider = fhirWebClientProvider;
    this.resultHandler = resultHandler;
  }

  /**
   * Package-visible helper that constructs the reconnector Runnable. Extracted for easier testing.
   *
   * @return a Runnable that attempts reconnection
   */
  private void reconnect() {
    if (connectingWebsocket.get()) {
      return;
    }
    logger.info("Websocket connection recovery attempt in {}ms ...", delay.get());
    executor.schedule(() -> {
      try {
        websocketConnectionEstablished.set(false);
        listenForQueryResults();
        delay.set(START_DELAY_MS);
        logger.info("Websocket connection recovered");
      } catch (Exception e) {
        logger.warn("Could not establish websocket connection: {}", e.getMessage());
        connectingWebsocket.set(false);
        reconnect();
      }
    }, delay.getAndUpdate(d -> Math.min(2 * d, MAX_DELAY_MS)), TimeUnit.MILLISECONDS);
  }

  private void listenForQueryResults() throws FhirWebClientProvisionException {
    if (!websocketConnectionEstablished.get() && connectingWebsocket.compareAndSet(false, true)) {
      WebsocketClient fhirWebsocketClient = fhirWebClientProvider.provideFhirWebsocketClient(this::reconnect);
      fhirWebsocketClient.setResourceHandler(this::setUpQueryResultHandler, this::setUpResourceParser);

      logger.debug("Establishing websocket connection ...");
      fhirWebsocketClient.connect();
      connectingWebsocket.set(false);
      websocketConnectionEstablished.set(true);
      logger.info("Websocket connection established");
    }
  }

  private void setUpQueryResultHandler(Resource resource) {
    resultHandler.onResult(resource).ifPresent((res) -> {
      store.storeResult(res);
      notifyResultListeners(res);
    });
  }

  private IParser setUpResourceParser() {
    return fhirContext.newJsonParser()
        .setStripVersionsFromReferences(false)
        .setOverrideResourceIdWithBundleEntryFullUrl(false);
  }

  private void notifyResultListeners(DSFQueryResult result) {
    for (Entry<DSFBrokerClient, QueryStatusListener> listener : listeners.entrySet()) {
      var broker = listener.getKey();
      var statusListener = listener.getValue();
      var statusUpdate = QueryStatusUpdate.builder()
          .source(broker)
          .brokerQueryId(result.getQueryId())
          .brokerSiteId(result.getSiteId())
          .status(QueryStatus.COMPLETED)
          .build();
      var associatedBackendQueryId = broker.getBackendQueryId(result.getQueryId());

      statusListener.onClientUpdate(associatedBackendQueryId, statusUpdate);
    }
  }

  @Override
  public void addResultListener(DSFBrokerClient broker, QueryStatusListener listener) throws IOException {
    listeners.put(broker, listener);
    try {
      listenForQueryResults();
    } catch (FhirWebClientProvisionException e) {
      listeners.remove(broker);
      throw new IOException("failed to establish websocket connection to listen for results", e);
    }
  }

  @Override
  public int getResultFeasibility(String queryId, String siteId) throws QueryNotFoundException, SiteNotFoundException {
    return store.getMeasureCount(queryId, siteId);
  }

  @Override
  public List<String> getResultSiteIds(String queryId) throws QueryNotFoundException {
    return store.getSiteIdsWithResult(queryId);
  }

  @Override
  public void removeResults(String queryId) throws QueryNotFoundException {
    store.removeResult(queryId);
  }

  @Override
  public void close() {
    executor.shutdownNow();
  }
}
