package de.fdpg.dataportal_backend.query.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fdpg.dataportal_backend.query.QueryMediaType;
import de.fdpg.dataportal_backend.query.api.StructuredQuery;
import de.fdpg.dataportal_backend.query.broker.BrokerClient;
import de.fdpg.dataportal_backend.query.broker.QueryNotFoundException;
import de.fdpg.dataportal_backend.query.broker.UnsupportedMediaTypeException;
import de.fdpg.dataportal_backend.query.persistence.*;
import de.fdpg.dataportal_backend.query.persistence.QueryDispatch.QueryDispatchId;
import de.fdpg.dataportal_backend.query.translation.QueryTranslationComponent;
import de.fdpg.dataportal_backend.query.translation.QueryTranslationException;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * Centralized component to enqueue and dispatch (publish) a {@link StructuredQuery}.
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
public class QueryDispatcher {

  @NonNull
  private List<BrokerClient> queryBrokerClients;

  @NonNull
  private QueryTranslationComponent queryTranslationComponent;

  @NonNull
  private QueryHashCalculator queryHashCalculator;

  @NonNull
  private ObjectMapper jsonUtil;

  @NonNull
  private QueryRepository queryRepository;

  @NonNull
  private QueryContentRepository queryContentRepository;

  @NonNull
  private QueryDispatchRepository queryDispatchRepository;

  /**
   * Enqueues a {@link StructuredQuery}, allowing it to be published afterwards. Enqueued queries are stored within
   * the database as a side effect.
   *
   * @param query  The query that shall be enqueued.
   * @param userId keycloak auth id of the author of the query
   * @return Identifier of the enqueued query. This acts as a reference when trying to publish it.
   * @throws QueryDispatchException If an error occurs while enqueueing the query.
   */
  public Long enqueueNewQuery(StructuredQuery query, String userId) throws QueryDispatchException {
    log.info("try to enqueue new query");
    var querySerialized = serializedStructuredQuery(query);

    var queryHash = queryHashCalculator.calculateSerializedQueryBodyHash(querySerialized);
    var queryBody = queryContentRepository.findByHash(queryHash)
        .orElseGet(() -> {
          var freshQueryBody = new QueryContent(querySerialized);
          freshQueryBody.setHash(queryHash);
          return queryContentRepository.save(freshQueryBody);
        });

    var queryId = persistEnqueuedQuery(queryBody, userId);
    log.info("enqueued query '{}'", queryId);
    return queryId;
  }

  /**
   * Dispatches (publishes) an already enqueued query in a broadcast fashion using all configured {@link BrokerClient}s.
   * The dispatch is designed to happen asynchronously (but not in parallel).
   *
   * @param queryId Identifies the backend query that shall be dispatched.
   * @return A {@link Mono} in complete state if at least a single broker managed to publish the query. If all brokers
   * failed to publish the query then a {@link Mono} in an error state is returned.
   */
  // TODO: Pass in audit information! (actor)
  public Mono<Void> dispatchEnqueuedQuery(Long queryId) {
    try {
      var enqueuedQuery = getEnqueuedQuery(queryId);
      var deserializedQueryBody = getStructuredQueryFromEnqueuedQuery(enqueuedQuery);
      var translatedQueryBodyFormats = translateQueryIntoTargetFormats(deserializedQueryBody);

      var dispatchable = new Dispatchable(enqueuedQuery, translatedQueryBodyFormats);

      var dispatches = queryBrokerClients.stream()
          .map(c -> dispatchAsynchronously(dispatchable, c)).toList();

      return Mono.zip(dispatches, dispatchResults -> Arrays.stream(dispatchResults)
              .allMatch(Predicate.isEqual(false)))
          .flatMap(allDispatchesFailed -> {
            if (allDispatchesFailed) {
              return Mono.error(new QueryDispatchException(("cannot dispatch query with id '%s'. " +
                  "Dispatch failed for all brokers").formatted(queryId)));
            } else {
              return Mono.empty();
            }
          })
          .then();
    } catch (QueryDispatchException e) {
      log.error("dispatch of query with id '%s' failed".formatted(queryId), e);
      return Mono.error(new QueryDispatchException("dispatch of query with id '%s' failed".formatted(queryId), e));
    }
  }

  /**
   * Dispatches a single dispatchable entity (query) using the specified broker.
   * Dispatching happens in an asynchronous fashion.
   *
   * @param dispatchable This is going to be dispatched.
   * @param broker       This actually dispatches the dispatchable entity.
   * @return A {@link Mono} holding the completion status of the dispatch operation that is either true
   * (dispatch was successful) or false (dispatch failed).
   */
  private Mono<Boolean> dispatchAsynchronously(Dispatchable dispatchable, BrokerClient broker) {
    return Mono.fromCallable(() -> {
      try {
        var brokerQueryId = broker.createQuery(dispatchable.query.getId());

        for (Entry<QueryMediaType, String> queryBodyFormats : dispatchable.serializedQueryByFormat.entrySet()) {
          broker.addQueryDefinition(brokerQueryId, queryBodyFormats.getKey(),
              queryBodyFormats.getValue());
        }
        broker.publishQuery(brokerQueryId);
        persistDispatchedQuery(dispatchable.query, brokerQueryId, broker.getBrokerType());
        log.info("dispatched query '{}' as '{}' with broker type '{}'", dispatchable.query.getId(),
            brokerQueryId, broker.getBrokerType());
        return true;
      } catch (UnsupportedMediaTypeException | QueryNotFoundException | IOException e) {
        log.error("failed to dispatch query '{}' with broker type '{}': {}",
            dispatchable.query.getId(), broker.getBrokerType(), e.getMessage());
        return false;
      }
    });
  }

  private String serializedStructuredQuery(StructuredQuery query) throws QueryDispatchException {
    try {
      return jsonUtil.writeValueAsString(query);
    } catch (JsonProcessingException e) {
      throw new QueryDispatchException("could not serialize query in order to enqueue it", e);
    }
  }

  private Long persistEnqueuedQuery(QueryContent queryBody, String userId) {
    var feasibilityQuery = new Query();
    feasibilityQuery.setCreatedAt(Timestamp.from(Instant.now()));
    feasibilityQuery.setCreatedBy(userId);
    feasibilityQuery.setQueryContent(queryBody);
    return queryRepository.save(feasibilityQuery).getId();
  }

  private void persistDispatchedQuery(Query query, String brokerInternalId, BrokerClientType brokerType) {
    var queryDispatchId = new QueryDispatchId();
    queryDispatchId.setQueryId(query.getId());
    queryDispatchId.setExternalId(brokerInternalId);
    queryDispatchId.setBrokerType(brokerType);

    var dispatchedQuery = new QueryDispatch();
    dispatchedQuery.setId(queryDispatchId);
    dispatchedQuery.setQuery(query);
    dispatchedQuery.setDispatchedAt(Timestamp.from(Instant.now()));
    queryDispatchRepository.save(dispatchedQuery);
  }

  private Query getEnqueuedQuery(Long queryId) throws QueryDispatchException {
    return queryRepository.findById(queryId)
        .orElseThrow(() ->
            new QueryDispatchException("cannot find enqueued query with id '%s'".formatted(queryId)));
  }

  private StructuredQuery getStructuredQueryFromEnqueuedQuery(Query enqueuedQuery) throws QueryDispatchException {
    try {
      return jsonUtil.readValue(enqueuedQuery.getQueryContent().getQueryContent(), StructuredQuery.class);
    } catch (JsonProcessingException e) {
      throw new QueryDispatchException("cannot deserialize enqueued query body as structured query", e);
    }
  }

  private Map<QueryMediaType, String> translateQueryIntoTargetFormats(StructuredQuery query)
      throws QueryDispatchException {
    try {
      return queryTranslationComponent.translate(query);
    } catch (QueryTranslationException e) {
      throw new QueryDispatchException("cannot translate enqueued query body into configured formats", e);
    }
  }

  @Getter
  @RequiredArgsConstructor
  private static class Dispatchable {

    @NonNull
    private Query query;

    @NonNull
    private Map<QueryMediaType, String> serializedQueryByFormat;
  }
}
