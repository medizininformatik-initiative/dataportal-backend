package de.fdpg.dataportal_backend.query.broker.mock;

import de.fdpg.dataportal_backend.query.QueryMediaType;
import de.fdpg.dataportal_backend.query.broker.QueryNotFoundException;
import de.fdpg.dataportal_backend.query.broker.SiteNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockBrokerClientTest {

    private static final Long TEST_BACKEND_QUERY_ID = 1L;

    MockBrokerClient client;

    @BeforeEach
    void setUp() {
        client = new MockBrokerClient();
    }

    @Test
    void testAddQueryDefinitionToNonExistingQuery() {
        assertDoesNotThrow(() -> client.addQueryDefinition("does-not-exist", QueryMediaType.JSON, ""));
    }

    @Test
    void testAddQueryDefinitionToExistingQuery() {
        var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        assertDoesNotThrow(() -> client.addQueryDefinition(queryId, QueryMediaType.JSON, ""));
    }

    @Test
    void testCloseQueryWhichDoesNotExist() {
        assertThrows(QueryNotFoundException.class, () -> client.closeQuery("does-not-exist"));
    }

    @Test
    void testCloseQueryWhichHasNotYetBeenPublished() {
        var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        assertDoesNotThrow(() -> client.closeQuery(queryId));
    }

    @Test
    void testGetResultFeasibilityForQueryWhichDoesNotExist() {
        assertThrows(QueryNotFoundException.class, () -> client.getResultFeasibility("does-not-exist", "foo"));
    }

    @Test
    void testGetResultFeasibilityForUnknownSite() {
        var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        assertThrows(SiteNotFoundException.class, () -> client.getResultFeasibility(queryId, "unknown-site-id"));
    }

    @Test
    void testGetResultSiteIdsForQueryWhichDoesNotExist() {
        assertThrows(QueryNotFoundException.class, () -> client.getResultSiteIds("does-not-exist"));
    }

    @Test
    void testGetResultSiteIdsForUnpublishedQuery() throws QueryNotFoundException {
        var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        var resultSiteIds = client.getResultSiteIds(queryId);

        assertTrue(resultSiteIds.isEmpty());
    }

    @Test
    void testGetSiteName() {
        assertEquals("Lübeck", client.getSiteName("2"));
        assertEquals("Erlangen", client.getSiteName("3"));
        assertEquals("Frankfurt", client.getSiteName("4"));
        assertEquals("Leipzig", client.getSiteName("5"));
        assertEquals("", client.getSiteName("6"));
        assertEquals("", client.getSiteName("foo"));
        assertEquals("", client.getSiteName("anything-other-than-2-to-5"));
    }
}
