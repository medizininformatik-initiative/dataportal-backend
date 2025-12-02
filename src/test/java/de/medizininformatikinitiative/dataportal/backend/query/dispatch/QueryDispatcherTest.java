package de.medizininformatikinitiative.dataportal.backend.query.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.medizininformatikinitiative.dataportal.backend.query.QueryMediaType;
import de.medizininformatikinitiative.dataportal.backend.query.api.StructuredQuery;
import de.medizininformatikinitiative.dataportal.backend.query.broker.BrokerClient;
import de.medizininformatikinitiative.dataportal.backend.query.broker.QueryDefinitionNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.broker.QueryNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.*;
import de.medizininformatikinitiative.dataportal.backend.query.translation.QueryTranslationComponent;
import de.medizininformatikinitiative.dataportal.backend.query.translation.QueryTranslationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

@Tag("query")
@Tag("dispatch")
@ExtendWith(MockitoExtension.class)
public class QueryDispatcherTest {

  @Mock
  private QueryTranslationComponent queryTranslationComponent;

  @Mock
  private QueryHashCalculator queryHashCalculator;

  @Spy
  private ObjectMapper jsonUtil = new ObjectMapper();

  @Mock
  private QueryRepository queryRepository;

  @Mock
  private QueryContentRepository queryContentRepository;

  @Mock
  private QueryDispatchRepository queryDispatchRepository;

  private QueryDispatcher createQueryDispatcher(List<BrokerClient> brokerClients) {
    return new QueryDispatcher(brokerClients, queryTranslationComponent, queryHashCalculator,
        jsonUtil, queryRepository, queryContentRepository, queryDispatchRepository);
  }


  @BeforeEach
  public void resetMocks() {
    Mockito.reset(queryTranslationComponent, queryHashCalculator, jsonUtil, queryRepository, queryContentRepository,
        queryDispatchRepository);
  }

  @Test
  public void testDispatchEnqueuedQuery_FailsWhenUnableToGetEnqueuedQuery() {
    var testQueryId = 99999L;
    doReturn(Optional.empty()).when(queryRepository).findById(testQueryId);

    var queryDispatcher = createQueryDispatcher(List.of());
    StepVerifier.create(queryDispatcher.dispatchEnqueuedQuery(testQueryId))
        .expectError(QueryDispatchException.class)
        .verify();
  }

  @Test
  public void testDispatchEnqueuedQuery_FailsWhenStructuredQueryNotFetchable() throws JsonProcessingException {
    var testQueryId = 99999L;
    var testQuery = new Query();
    testQuery.setId(testQueryId);
    var testQueryContent = new QueryContent(jsonUtil.writeValueAsString(StructuredQuery.builder().build()));
    testQuery.setQueryContent(testQueryContent);

    doReturn(Optional.of(testQuery)).when(queryRepository).findById(testQueryId);
    doThrow(JsonProcessingException.class).when(jsonUtil).readValue(testQueryContent.getQueryContent(),
        StructuredQuery.class);

    var queryDispatcher = createQueryDispatcher(List.of());
    StepVerifier.create(queryDispatcher.dispatchEnqueuedQuery(testQueryId))
        .expectError(QueryDispatchException.class)
        .verify();
  }

  @Test
  public void testDispatchEnqueuedQuery_FailsWhenQueryCannotGetTranslated() throws JsonProcessingException,
      QueryTranslationException {
    var testQueryId = 99999L;
    var testQuery = new Query();
    testQuery.setId(testQueryId);
    var structuredQuery = StructuredQuery.builder().build();
    var testQueryContent = new QueryContent(jsonUtil.writeValueAsString(structuredQuery));
    testQuery.setQueryContent(testQueryContent);

    doReturn(Optional.of(testQuery)).when(queryRepository).findById(testQueryId);
    doReturn(structuredQuery).when(jsonUtil).readValue(testQueryContent.getQueryContent(), StructuredQuery.class);
    doThrow(QueryTranslationException.class).when(queryTranslationComponent).translate(structuredQuery);


    var queryDispatcher = createQueryDispatcher(List.of());
    StepVerifier.create(queryDispatcher.dispatchEnqueuedQuery(testQueryId))
        .expectError(QueryDispatchException.class)
        .verify();
  }

  @Test
  public void testDispatchEnqueuedQuery_FailsOnBrokerQueryCreationError() throws IOException,
      QueryTranslationException {
    var testQueryId = 99999L;
    var testQuery = new Query();
    testQuery.setId(testQueryId);
    var structuredQuery = StructuredQuery.builder().build();
    var testQueryContent = new QueryContent(jsonUtil.writeValueAsString(structuredQuery));
    testQuery.setQueryContent(testQueryContent);

    var failingBrokerClient = mock(BrokerClient.class);

    doReturn(Optional.of(testQuery)).when(queryRepository).findById(testQueryId);
    doReturn(structuredQuery).when(jsonUtil).readValue(testQueryContent.getQueryContent(), StructuredQuery.class);
    doReturn(Map.of()).when(queryTranslationComponent).translate(structuredQuery);
    doThrow(IOException.class).when(failingBrokerClient).createQuery(testQueryId);

    var queryDispatcher = createQueryDispatcher(List.of(failingBrokerClient));
    StepVerifier.create(queryDispatcher.dispatchEnqueuedQuery(testQueryId))
        .expectError(QueryDispatchException.class)
        .verify();
  }

  @Test
  public void testDispatchEnqueuedQuery_FailsOnBrokerPublishError()
      throws IOException, QueryTranslationException,
      QueryNotFoundException, QueryDefinitionNotFoundException {
    var testQueryId = 99999L;
    var testQuery = new Query();
    testQuery.setId(testQueryId);
    var structuredQuery = StructuredQuery.builder()
        .version(URI.create("https://to.be.decided/schema"))
        .display("Test")
        .build();

    var testQueryContent = new QueryContent(jsonUtil.writeValueAsString(structuredQuery));
    testQuery.setQueryContent(testQueryContent);
    var translationResult = Map.of(QueryMediaType.STRUCTURED_QUERY, testQueryContent.getQueryContent());

    var failingBrokerClient = mock(BrokerClient.class);

    doReturn(Optional.of(testQuery)).when(queryRepository).findById(testQueryId);
    doReturn(structuredQuery).when(jsonUtil).readValue(testQueryContent.getQueryContent(), StructuredQuery.class);
    doReturn(translationResult).when(queryTranslationComponent).translate(structuredQuery);
    doReturn("1").when(failingBrokerClient).createQuery(testQueryId);
    doThrow(IOException.class).when(failingBrokerClient).publishQuery("1");

    var queryDispatcher = createQueryDispatcher(List.of(failingBrokerClient));
    StepVerifier.create(queryDispatcher.dispatchEnqueuedQuery(testQueryId))
        .expectError(QueryDispatchException.class)
        .verify();
  }

  @Test
  public void testDispatchEnqueuedQuery_DoesNotFailOnSingleBrokerError()
      throws IOException, QueryNotFoundException, QueryDefinitionNotFoundException {
    var failingBrokerClient = mock(BrokerClient.class);
    var succeedingBrokerClient = mock(BrokerClient.class);
    var queryDispatcher = createQueryDispatcher(List.of(failingBrokerClient, succeedingBrokerClient));

    var testQueryId = 99999L;
    var testQuery = new Query();
    testQuery.setId(testQueryId);
    var testQueryContent = new QueryContent(jsonUtil.writeValueAsString(StructuredQuery.builder().build()));
    testQuery.setQueryContent(testQueryContent);

    doReturn(Optional.of(testQuery)).when(queryRepository).findById(testQueryId);
    doReturn("1").when(failingBrokerClient).createQuery(testQueryId);
    doReturn("1").when(succeedingBrokerClient).createQuery(testQueryId);
    doThrow(IOException.class).when(failingBrokerClient).publishQuery(anyString());


    StepVerifier.create(queryDispatcher.dispatchEnqueuedQuery(testQueryId))
        .expectComplete()
        .verify();
    verify(failingBrokerClient, times(1)).publishQuery("1");
    verify(succeedingBrokerClient, times(1)).publishQuery("1");
  }

  @Test
  public void testDispatchEnqueuedQuery_BrokerAfterFirstSuccessfulOneAreCalled()
      throws IOException,
      QueryNotFoundException, QueryDefinitionNotFoundException {
    var failingBrokerClient = mock(BrokerClient.class);
    var succeedingBrokerClient = mock(BrokerClient.class);
    var queryDispatcher = createQueryDispatcher(List.of(succeedingBrokerClient, failingBrokerClient));

    var testQueryId = 99999L;
    var testQuery = new Query();
    testQuery.setId(testQueryId);
    var testQueryContent = new QueryContent(jsonUtil.writeValueAsString(StructuredQuery.builder().build()));
    testQuery.setQueryContent(testQueryContent);

    doReturn(Optional.of(testQuery)).when(queryRepository).findById(testQueryId);
    doReturn("1").when(succeedingBrokerClient).createQuery(testQueryId);
    doReturn("1").when(failingBrokerClient).createQuery(testQueryId);
    doThrow(IOException.class).when(failingBrokerClient).publishQuery(anyString());


    StepVerifier.create(queryDispatcher.dispatchEnqueuedQuery(testQueryId))
        .expectComplete()
        .verify();
    verify(failingBrokerClient, times(1)).publishQuery("1");
    verify(succeedingBrokerClient, times(1)).publishQuery("1");
  }

  @Test
  public void testDispatchEnqueuedQuery_DoesFailIfAllBrokersFail()
      throws IOException, QueryNotFoundException, QueryDefinitionNotFoundException {
    var failingBrokerClient = mock(BrokerClient.class);
    var anotherFailingBrokerClient = mock(BrokerClient.class);
    var queryDispatcher = createQueryDispatcher(List.of(failingBrokerClient,
        anotherFailingBrokerClient));

    var testQueryId = 99999L;
    var testQuery = new Query();
    testQuery.setId(testQueryId);
    var testQueryContent = new QueryContent(jsonUtil.writeValueAsString(StructuredQuery.builder().build()));
    testQuery.setQueryContent(testQueryContent);

    doReturn(Optional.of(testQuery)).when(queryRepository).findById(testQueryId);
    doReturn("1").when(failingBrokerClient).createQuery(testQueryId);
    doReturn("1").when(anotherFailingBrokerClient).createQuery(testQueryId);
    doThrow(IOException.class).when(failingBrokerClient).publishQuery(anyString());
    doThrow(IOException.class).when(anotherFailingBrokerClient).publishQuery(anyString());

    StepVerifier.create(queryDispatcher.dispatchEnqueuedQuery(testQueryId))
        .expectError(QueryDispatchException.class)
        .verify();
    verify(failingBrokerClient, times(1)).publishQuery("1");
    verify(anotherFailingBrokerClient, times(1)).publishQuery("1");
  }
}
