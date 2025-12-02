package de.fdpg.dataportal_backend.query.broker.direct;

import de.fdpg.dataportal_backend.query.QueryMediaType;
import de.fdpg.dataportal_backend.query.broker.QueryDefinitionNotFoundException;
import de.fdpg.dataportal_backend.query.broker.QueryNotFoundException;
import de.fdpg.dataportal_backend.query.broker.SiteNotFoundException;
import de.fdpg.dataportal_backend.query.collect.QueryStatusListener;
import de.fdpg.dataportal_backend.query.collect.QueryStatusUpdate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static de.fdpg.dataportal_backend.query.broker.direct.DirectBrokerClient.SITE_ID_LOCAL;
import static de.fdpg.dataportal_backend.query.collect.QueryStatus.COMPLETED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DirectBrokerClientCqlTest {

  private static final Long TEST_BACKEND_QUERY_ID = 1234325L;
  private static final String EXAMPLE_CQL = "example cql";
  private static final int MEASURE_COUNT = 8723132;

  @SuppressWarnings("unused")
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  FhirConnector fhirConnector;

  @SuppressWarnings("unused")
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  FhirHelper fhirHelper;

  DirectBrokerClientCql client;

  @BeforeEach
  void setUp() {
    client = new DirectBrokerClientCql(fhirConnector, false, fhirHelper);
  }

  @Test
  void testPublishExistingQuerySuccess() throws Exception {
    var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
    client.addQueryDefinition(queryId, QueryMediaType.CQL, EXAMPLE_CQL);
    Bundle testBundle = new Bundle();
    MeasureReport measureReport = new MeasureReport();
    measureReport.addGroup().addPopulation().setCount(MEASURE_COUNT);
    when(fhirHelper.createBundle(eq(EXAMPLE_CQL), any(String.class), any(String.class))).thenReturn(testBundle);
    when(fhirConnector.evaluateMeasure(any(String.class))).thenReturn(measureReport);

    client.publishQuery(queryId);

    verify(fhirConnector).transmitBundle(testBundle);
    assertEquals(MEASURE_COUNT, client.findQuery(queryId).getResult());
  }

  @Test
  void testPublishExistingQuerySuccessListener() throws Exception {
    var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
    client.addQueryDefinition(queryId, QueryMediaType.CQL, EXAMPLE_CQL);
    var listener = mock(QueryStatusListener.class);
    client.addQueryStatusListener(listener);
    Bundle testBundle = new Bundle();
    MeasureReport measureReport = new MeasureReport();
    measureReport.addGroup().addPopulation().setCount(MEASURE_COUNT);
    when(fhirHelper.createBundle(eq(EXAMPLE_CQL), any(String.class), any(String.class))).thenReturn(testBundle);
    when(fhirConnector.evaluateMeasure(any(String.class))).thenReturn(measureReport);

    client.publishQuery(queryId);

    verify(listener).onClientUpdate(
        TEST_BACKEND_QUERY_ID,
        QueryStatusUpdate.builder()
            .source(client)
            .brokerQueryId(queryId)
            .brokerSiteId(SITE_ID_LOCAL)
            .status(COMPLETED)
            .build()
    );
  }


  @Test
  void testPublishNonExistingQuery() {
    assertThrows(QueryNotFoundException.class, () -> client.publishQuery("does-not-exist"));
  }

  @Test
  void testPublishExistingQueryWithoutQueryDefinition() {
    var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);

    assertThrows(QueryDefinitionNotFoundException.class, () -> client.publishQuery(queryId));
  }

  @Test
  void testPublishExistingQueryWithIOExceptionInCreateBundle() throws Exception {
    var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
    client.addQueryDefinition(queryId, QueryMediaType.CQL, EXAMPLE_CQL);
    when(fhirHelper.createBundle(eq(EXAMPLE_CQL), any(String.class), any(String.class))).thenThrow(IOException.class);

    assertThrows(IOException.class, () -> client.publishQuery(queryId));
  }

  @Test
  void testPublishExistingQueryWithIOExceptionInTransmitBundle() throws Exception {
    var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
    client.addQueryDefinition(queryId, QueryMediaType.CQL, EXAMPLE_CQL);
    doThrow(IOException.class).when(fhirConnector).transmitBundle(any(Bundle.class));

    assertThrows(IOException.class, () -> client.publishQuery(queryId));
  }

  @Test
  void testPublishExistingQueryWithIOExceptionInEvaluateMeasure() throws Exception {
    var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
    client.addQueryDefinition(queryId, QueryMediaType.CQL, EXAMPLE_CQL);
    when(fhirConnector.evaluateMeasure(any(String.class))).thenThrow(IOException.class);

    assertThrows(IOException.class, () -> client.publishQuery(queryId));
  }

  @Test
  void testGetSiteName() throws Exception {
    assertEquals("Local Server", client.getSiteName("1"));
    assertThrows(SiteNotFoundException.class, () -> client.getSiteName("foo"));
    assertThrows(SiteNotFoundException.class, () -> client.getSiteName("something-else"));
    assertThrows(SiteNotFoundException.class, () -> client.getSiteName("FHIR Server"));
  }

  @Test
  void testCloseQueryWhichDoesNotExist() {
    assertThrows(QueryNotFoundException.class, () -> client.closeQuery("does-not-exist"));
  }

  @Test
  void testCloseQuery() throws Exception {
    var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);

    client.closeQuery(queryId);

    assertThrows(QueryNotFoundException.class, () -> client.findQuery(queryId));
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
