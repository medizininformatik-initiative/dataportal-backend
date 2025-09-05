package de.numcodex.feasibility_gui_backend.query.broker.dsf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatus;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusUpdate;
import dev.dsf.fhir.client.WebsocketClient;
import org.hl7.fhir.r4.model.Resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Collector for collecting the results of feasibility queries that are running in a distributed fashion.
 * <p>
 * The collector gathers query results from a single FHIR server. Communication with this FHIR server
 * happens using a websocket. The FHIR server sends all task resources that are associated with a subscription.
 */
class DSFQueryResultCollector implements QueryResultCollector {

    private final QueryResultStore store;
    private final FhirContext fhirContext;
    private final FhirWebClientProvider fhirWebClientProvider;
    private final DSFQueryResultHandler resultHandler;
    private final Map<DSFBrokerClient, QueryStatusListener> listeners;
    private boolean websocketConnectionEstablished;

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
        this.websocketConnectionEstablished = false;
        this.listeners = new HashMap<>();
    }

    private void listenForQueryResults() throws FhirWebClientProvisionException {
        if (!websocketConnectionEstablished) {
            WebsocketClient fhirWebsocketClient = fhirWebClientProvider.provideFhirWebsocketClient();
            fhirWebsocketClient.setResourceHandler(this::setUpQueryResultHandler, this::setUpResourceParser);

            fhirWebsocketClient.connect();
            websocketConnectionEstablished = true;
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
}
