package de.fdpg.dataportal_backend.query.broker.direct;

import de.fdpg.dataportal_backend.query.broker.QueryDefinitionNotFoundException;
import de.fdpg.dataportal_backend.query.broker.QueryNotFoundException;
import de.fdpg.dataportal_backend.query.broker.SiteNotFoundException;
import de.fdpg.dataportal_backend.query.collect.QueryStatusListener;
import de.fdpg.dataportal_backend.query.collect.QueryStatusUpdate;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static de.fdpg.dataportal_backend.query.QueryMediaType.STRUCTURED_QUERY;
import static de.fdpg.dataportal_backend.query.collect.QueryStatus.COMPLETED;
import static de.fdpg.dataportal_backend.query.collect.QueryStatus.FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NewClassNamingConvention")
class DirectBrokerClientFlareIT {

    private static final int ASYNC_TIMEOUT_WAIT_MS = 2000;
    private static final Long TEST_BACKEND_QUERY_ID = 1L;

    DirectBrokerClientFlare client;
    WebClient webClient;
    MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        webClient = WebClient.create(mockWebServer.url("/").toString());
        client = new DirectBrokerClientFlare(webClient, false);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testPublishQuery()
        throws QueryNotFoundException, IOException, InterruptedException, SiteNotFoundException, QueryDefinitionNotFoundException {
        var brokerQueryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        client.addQueryDefinition(brokerQueryId, STRUCTURED_QUERY, "foo");

        mockWebServer.enqueue(new MockResponse().setBody("123").setHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        var statusListener = mock(QueryStatusListener.class);
        client.addQueryStatusListener(statusListener);
        client.publishQuery(brokerQueryId);
        var recordedRequest = mockWebServer.takeRequest();

        assertEquals("application/sq+json", recordedRequest.getHeader(CONTENT_TYPE));
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("foo", recordedRequest.getBody().readUtf8());

        var statusUpdate = QueryStatusUpdate.builder()
                .source(client)
                .brokerQueryId(brokerQueryId)
                .brokerSiteId("1")
                .status(COMPLETED)
                .build();
        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS)).onClientUpdate(TEST_BACKEND_QUERY_ID, statusUpdate);

        assertEquals(1, client.getResultSiteIds(brokerQueryId).size());
        assertEquals("1", client.getResultSiteIds(brokerQueryId).get(0));
        assertEquals(123, client.getResultFeasibility(brokerQueryId, "1"));
    }

    @Test
    void testPublishQueryServerError()
        throws QueryNotFoundException, IOException, QueryDefinitionNotFoundException {
        var brokerQueryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        client.addQueryDefinition(brokerQueryId, STRUCTURED_QUERY, "foo");

        mockWebServer.enqueue(new MockResponse().setStatus(INTERNAL_SERVER_ERROR.toString()));

        var statusListener = mock(QueryStatusListener.class);
        client.addQueryStatusListener(statusListener);
        client.publishQuery(brokerQueryId);

        var statusUpdate = QueryStatusUpdate.builder()
                .source(client)
                .brokerQueryId(brokerQueryId)
                .brokerSiteId("1")
                .status(FAILED)
                .build();
        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS).only()).onClientUpdate(TEST_BACKEND_QUERY_ID,
            statusUpdate);
    }

    @Test
    void testPublishQueryUnexpectedResponseBody()
        throws QueryNotFoundException, IOException, QueryDefinitionNotFoundException {
        var brokerQueryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        client.addQueryDefinition(brokerQueryId, STRUCTURED_QUERY, "foo");

        mockWebServer.enqueue(new MockResponse().setBody("not-a-number").setHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        var statusListener = mock(QueryStatusListener.class);
        client.addQueryStatusListener(statusListener);
        client.publishQuery(brokerQueryId);

        var statusUpdate = QueryStatusUpdate.builder()
                .source(client)
                .brokerQueryId(brokerQueryId)
                .brokerSiteId("1")
                .status(FAILED)
                .build();
        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS).only()).onClientUpdate(TEST_BACKEND_QUERY_ID,
            statusUpdate);
    }
}
