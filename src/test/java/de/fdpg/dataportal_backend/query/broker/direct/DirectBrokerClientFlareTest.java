package de.fdpg.dataportal_backend.query.broker.direct;

import de.fdpg.dataportal_backend.query.broker.QueryDefinitionNotFoundException;
import de.fdpg.dataportal_backend.query.broker.QueryNotFoundException;
import de.fdpg.dataportal_backend.query.broker.SiteNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DirectBrokerClientFlareTest {

  private static final Long TEST_BACKEND_QUERY_ID = 1L;

  @SuppressWarnings("unused")
  @Mock
  WebClient webClient;

  DirectBrokerClientFlare client;

  @BeforeEach
  void setUp() {
    client = new DirectBrokerClientFlare(webClient, false);

  }

  @Test
  void testPublishNonExistingQuery() {
    assertThrows(QueryNotFoundException.class, () -> client.publishQuery("does-not-exist"));
  }

  @Test
  void testPublishExistingQueryWithoutStructuredQueryDefinition() {
    var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
    assertThrows(QueryDefinitionNotFoundException.class, () -> client.publishQuery(queryId));
  }

  @Test
  void testGetSiteName() {
    assertDoesNotThrow(() -> assertEquals("Local Server", client.getSiteName("1")));
    assertThrows(SiteNotFoundException.class, () -> client.getSiteName("foo"));
    assertThrows(SiteNotFoundException.class, () -> client.getSiteName("something-else"));
    assertThrows(SiteNotFoundException.class, () -> client.getSiteName("CQL Server"));
  }

  @Test
  void testCloseQueryWhichDoesNotExist() {
    assertThrows(QueryNotFoundException.class, () -> client.closeQuery("does-not-exist"));
  }

  @Test
  void testCloseQuery() {
    var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
    assertDoesNotThrow(() -> client.closeQuery(queryId));
  }

  @Test
  void testGetResultFeasibilityForQueryWhichDoesNotExist() {
    assertThrows(QueryNotFoundException.class, () -> client.getResultFeasibility("does-not-exist", "unknown-site-id"));
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
}
