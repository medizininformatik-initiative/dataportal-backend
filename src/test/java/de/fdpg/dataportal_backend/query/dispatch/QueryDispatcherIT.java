package de.fdpg.dataportal_backend.query.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fdpg.dataportal_backend.query.api.StructuredQuery;
import de.fdpg.dataportal_backend.query.broker.BrokerClient;
import de.fdpg.dataportal_backend.query.broker.BrokerSpringConfig;
import de.fdpg.dataportal_backend.query.collect.QueryCollectSpringConfig;
import de.fdpg.dataportal_backend.query.persistence.QueryContent;
import de.fdpg.dataportal_backend.query.persistence.QueryContentRepository;
import de.fdpg.dataportal_backend.query.persistence.QueryDispatchRepository;
import de.fdpg.dataportal_backend.query.persistence.QueryRepository;
import de.fdpg.dataportal_backend.query.result.ResultService;
import de.fdpg.dataportal_backend.query.result.ResultServiceSpringConfig;
import de.fdpg.dataportal_backend.query.translation.QueryTranslatorSpringConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.List;

import static de.fdpg.dataportal_backend.query.persistence.BrokerClientType.MOCK;
import static org.junit.jupiter.api.Assertions.*;

@Tag("query")
@Tag("dispatch")
@Import({
    QueryTranslatorSpringConfig.class,
    QueryDispatchSpringConfig.class,
    QueryCollectSpringConfig.class,
    BrokerSpringConfig.class,
    ResultServiceSpringConfig.class
})
@DataJpaTest(
    properties = {
        "app.cqlTranslationEnabled=false",
        "app.fhirTranslationEnabled=false",
        "app.broker.mock.enabled=true",
        "app.broker.direct.enabled=false",
        "app.broker.aktin.enabled=false",
        "app.broker.dsf.enabled=false"
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@SuppressWarnings("NewClassNamingConvention")
public class QueryDispatcherIT {

  @Autowired
  private QueryDispatcher queryDispatcher;

  @Autowired
  private QueryRepository queryRepository;

  @Autowired
  private QueryDispatchRepository queryDispatchRepository;

  @Autowired
  private QueryContentRepository queryContentRepository;


  @Autowired
  private ResultService resultService;

  @Autowired
  @Qualifier("translation")
  private ObjectMapper jsonUtil;

  @Autowired
  private QueryHashCalculator queryHashCalculator;

  @Autowired
  private List<BrokerClient> queryBrokerClients;

  @Test
  public void testEnqueueNewQuery_QueryContentGetsCreatedIfNotAlreadyPresent() throws JsonProcessingException {
    var otherQuery = StructuredQuery.builder()
        .version(URI.create("https://to_be_decided.com/draft-2/schema#"))
        .build();
    var serializedOtherQuery = jsonUtil.writeValueAsString(otherQuery);
    var serializedOtherQueryHash = queryHashCalculator.calculateSerializedQueryBodyHash(serializedOtherQuery);

    var otherQueryContent = new QueryContent(serializedOtherQuery);
    otherQueryContent.setHash(serializedOtherQueryHash);
    queryContentRepository.save(otherQueryContent);


    var testQuery = StructuredQuery.builder().build();
    var serializedTestQuery = jsonUtil.writeValueAsString(testQuery);
    var serializedTestQueryHash = queryHashCalculator.calculateSerializedQueryBodyHash(serializedTestQuery);

    assertDoesNotThrow(() -> queryDispatcher.enqueueNewQuery(testQuery, "test"));

    var queryContent = queryContentRepository.findByHash(serializedTestQueryHash);
    assertEquals(2, queryContentRepository.count());
    assertTrue(queryContent.isPresent());
    assertEquals(serializedTestQuery, queryContent.get().getQueryContent());
  }

  @Test
  public void testEnqueueNewQuery_QueryContentGetsReusedIfAlreadyPresent() throws JsonProcessingException {
    var testQuery = StructuredQuery.builder().build();
    var serializedTestQuery = jsonUtil.writeValueAsString(testQuery);
    var serializedTestQueryHash = queryHashCalculator.calculateSerializedQueryBodyHash(serializedTestQuery);

    var queryContent = new QueryContent(serializedTestQuery);
    queryContent.setHash(serializedTestQueryHash);
    queryContentRepository.save(queryContent);

    assertDoesNotThrow(() -> queryDispatcher.enqueueNewQuery(testQuery, "test"));
    assertEquals(1, queryContentRepository.count());
  }

  @Test
  public void testEnqueueNewQuery() throws JsonProcessingException {
    var testQuery = StructuredQuery.builder().build();
    var serializedTestQuery = jsonUtil.writeValueAsString(testQuery);
    var serializedTestQueryHash = queryHashCalculator.calculateSerializedQueryBodyHash(serializedTestQuery);

    assertDoesNotThrow(() -> queryDispatcher.enqueueNewQuery(testQuery, "test"));

    var enqueuedQueries = queryRepository.findAll();
    assertEquals(1, enqueuedQueries.size());
    assertEquals(serializedTestQuery, enqueuedQueries.get(0).getQueryContent().getQueryContent());
    assertEquals(serializedTestQueryHash, enqueuedQueries.get(0).getQueryContent().getHash());
    assertNotNull(enqueuedQueries.get(0).getCreatedAt());
  }

  @Test
  public void testDispatchEnqueuedQuery_UnknownQueryIdDoesNotLeadToPersistedDispatchEntry() {
    var unknownQueryId = 9999999L;

    StepVerifier.create(queryDispatcher.dispatchEnqueuedQuery(unknownQueryId))
        .expectError(QueryDispatchException.class)
        .verify();
    assertEquals(0, queryDispatchRepository.count());
  }

  @Test
  public void testDispatchEnqueuedQuery() {
    var testQuery = StructuredQuery.builder().build();

    var queryId = assertDoesNotThrow(() -> queryDispatcher.enqueueNewQuery(testQuery, "test"));

    StepVerifier.create(queryDispatcher.dispatchEnqueuedQuery(queryId))
        .verifyComplete();

    var dispatches = queryDispatchRepository.findAll();
    assertEquals(1, dispatches.size());
    assertEquals(MOCK, dispatches.get(0).getId().getBrokerType());
    assertEquals(queryId, dispatches.get(0).getId().getQueryId());
    assertNotNull(dispatches.get(0).getDispatchedAt());
    assertNotNull(dispatches.get(0).getQuery());
  }
}
