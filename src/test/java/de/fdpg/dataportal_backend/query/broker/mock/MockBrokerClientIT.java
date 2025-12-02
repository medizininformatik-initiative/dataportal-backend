package de.fdpg.dataportal_backend.query.broker.mock;

import de.fdpg.dataportal_backend.query.broker.QueryNotFoundException;
import de.fdpg.dataportal_backend.query.broker.SiteNotFoundException;
import de.fdpg.dataportal_backend.query.collect.QueryStatusListener;
import de.fdpg.dataportal_backend.query.collect.QueryStatusUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static de.fdpg.dataportal_backend.query.collect.QueryStatus.COMPLETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SuppressWarnings("NewClassNamingConvention")
public class MockBrokerClientIT {

    private static final int ASYNC_TIMEOUT_WAIT_MS = 15000;
    private static final Long TEST_BACKEND_QUERY_ID = 1L;

    MockBrokerClient client;

    @BeforeEach
    void setUp() {
        client = new MockBrokerClient();
    }


    @Test
    void testPublishQuery() throws QueryNotFoundException, SiteNotFoundException {
        var brokerQueryId = client.createQuery(TEST_BACKEND_QUERY_ID);

        var statusListener = mock(QueryStatusListener.class);
        client.addQueryStatusListener(statusListener);
        client.publishQuery(brokerQueryId);

        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS).atLeastOnce()).onClientUpdate(
                TEST_BACKEND_QUERY_ID,
                QueryStatusUpdate.builder()
                        .source(client)
                        .brokerQueryId(brokerQueryId)
                        .brokerSiteId("2")
                        .status(COMPLETED)
                        .build()
        );
        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS).atLeastOnce()).onClientUpdate(
                TEST_BACKEND_QUERY_ID,
                QueryStatusUpdate.builder()
                        .source(client)
                        .brokerQueryId(brokerQueryId)
                        .brokerSiteId("3")
                        .status(COMPLETED)
                        .build()
        );
        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS).atLeastOnce()).onClientUpdate(
                TEST_BACKEND_QUERY_ID,
                QueryStatusUpdate.builder()
                        .source(client)
                        .brokerQueryId(brokerQueryId)
                        .brokerSiteId("4")
                        .status(COMPLETED)
                        .build()
        );
        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS).atLeastOnce()).onClientUpdate(
                TEST_BACKEND_QUERY_ID,
                QueryStatusUpdate.builder()
                        .source(client)
                        .brokerQueryId(brokerQueryId)
                        .brokerSiteId("5")
                        .status(COMPLETED)
                        .build()
        );

        assertEquals(4, client.getResultSiteIds(brokerQueryId).size());
        assertTrue(client.getResultFeasibility(brokerQueryId, "2") >= 10);
        assertTrue(client.getResultFeasibility(brokerQueryId, "3") >= 10);
        assertTrue(client.getResultFeasibility(brokerQueryId, "4") >= 10);
        assertTrue(client.getResultFeasibility(brokerQueryId, "5") >= 10);
    }

    @Test
    void testCloseQueryWhichIsRunning() throws QueryNotFoundException {
        var brokerQueryId = client.createQuery(TEST_BACKEND_QUERY_ID);

        var statusListener = mock(QueryStatusListener.class);
        client.addQueryStatusListener(statusListener);
        client.publishQuery(brokerQueryId);
        client.closeQuery(brokerQueryId);

        verify(statusListener, never()).onClientUpdate(
                TEST_BACKEND_QUERY_ID,
                QueryStatusUpdate.builder()
                        .source(client)
                        .brokerQueryId(brokerQueryId)
                        .brokerSiteId("1")
                        .status(COMPLETED)
                        .build()
        );
        verify(statusListener, never()).onClientUpdate(
                TEST_BACKEND_QUERY_ID,
                QueryStatusUpdate.builder()
                        .source(client)
                        .brokerQueryId(brokerQueryId)
                        .brokerSiteId("2")
                        .status(COMPLETED)
                        .build()
        );
        verify(statusListener, never()).onClientUpdate(
                TEST_BACKEND_QUERY_ID,
                QueryStatusUpdate.builder()
                        .source(client)
                        .brokerQueryId(brokerQueryId)
                        .brokerSiteId("3")
                        .status(COMPLETED)
                        .build()
        );
        verify(statusListener, never()).onClientUpdate(
                TEST_BACKEND_QUERY_ID,
                QueryStatusUpdate.builder()
                        .source(client)
                        .brokerQueryId(brokerQueryId)
                        .brokerSiteId("4")
                        .status(COMPLETED)
                        .build()
        );
    }
}
