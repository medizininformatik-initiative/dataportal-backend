package de.fdpg.dataportal_backend.query.result;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import de.fdpg.dataportal_backend.query.persistence.BrokerClientType;
import de.fdpg.dataportal_backend.query.persistence.QueryDispatchRepository;
import de.fdpg.dataportal_backend.query.persistence.ResultType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.aktin.broker.client2.BrokerAdmin2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.PeriodDuration;

/**
 * Stores results of queries volatile.
 * <p>
 * Query results are stored in form of {@link ResultLine result lines}, one for
 * each site per query. All results of a query are cleared after a configurable
 * duration. Only the first result per query will be kept.
 */
@Slf4j
public class ResultService {

  private static final Logger resultLogger = LoggerFactory.getLogger("resultLogger");

  private final Cache<Long, QueryResult> queryResultCache;
  private final BrokerAdmin2 aktinBrokerClient;

  @NonNull
  private final QueryDispatchRepository queryDispatchRepository;

  /**
   * Creates a new ResultService.
   *
   * @param resultExpiry the duration after which a result shouldn't be
   *                     available anymore
   */
  public ResultService(PeriodDuration resultExpiry, BrokerAdmin2 aktinBrokerClient,
                       QueryDispatchRepository queryDispatchRepository) {
    this.queryDispatchRepository = queryDispatchRepository;
    this.aktinBrokerClient = aktinBrokerClient;
    this.queryResultCache = Caffeine.newBuilder()
        .expireAfterWrite(resultExpiry.getDuration())
        .removalListener((key, value, cause) -> onRemoval(key, cause))
        .build();
  }

  /**
   * Deletes Query from AKTIN broker if the query has been dispatched to AKTIN
   * Broker.
   *
   * @param key the query id as saved in the cache
   */
  private void onRemoval(Object key, Object cause) {

    if (this.aktinBrokerClient == null || ! cause.equals(RemovalCause.EXPIRED)) {
      return;
    }

    Long queryId = (Long) key;
    var queryDispatch = queryDispatchRepository.findByQueryIdAndBrokerType(
        Long.toString(queryId), BrokerClientType.AKTIN);

    queryDispatch.ifPresent((queryDispatchResult) -> {
      String externalQueryId = queryDispatchResult.getId().getExternalId();
      log.debug(
          "Deleting query with internal ID {} and AKTIN ID {} from AKTIN broker",
          queryId, externalQueryId);
      try {
        aktinBrokerClient.deleteRequest(Integer.parseInt(externalQueryId));
      } catch (Exception e) {
        log.error(
            "Could not delete query internal ID {} and AKTIN ID {} from AKTIN broker",
            queryId, externalQueryId);
      }
    });

  }

  /**
   * Finds all {@link ResultLine results} for a query, that have been
   * successful.
   * <p>
   * In case the query is not found, or the query has no results, an empty
   * {@link List} is returned.
   *
   * @param queryId the query id
   * @return the list of all {@link ResultLine results} for the given {@code
   * queryId}, that have the {@link ResultType result type} {@link
   * ResultType#SUCCESS success}
   */
  public List<ResultLine> findSuccessfulByQuery(long queryId) {
    return queryResultCache.asMap().getOrDefault(queryId, QueryResult.EMPTY)
        .resultsBySite()
        .values().stream()
        .filter(resultLine -> resultLine.type() == ResultType.SUCCESS).toList();
  }

  /**
   * Adds {@code result} to the results of the query with {@code queryId}.
   * <p>
   * Note: Only the <b>first</b> {@link ResultLine result} per site is kept.
   * Subsequent results will be discarded.
   *
   * @param queryId the query id
   * @param result  the {@link ResultLine result} from a site
   */
  public void addResultLine(Long queryId, ResultLine result) {
    queryResultCache.asMap()
        .merge(queryId, QueryResult.ofResultLine(result), QueryResult::merge);
    resultLogger.info("{};{};{}", queryId, result.siteName(), result.result());
  }

  /**
   * Holds all submitted {@link ResultLine results} from sites to a query.
   */
  @Builder
  private record QueryResult(Map<String, ResultLine> resultsBySite) {

    private static final QueryResult EMPTY = QueryResult.builder()
            .resultsBySite(Map.of())
            .build();

    private QueryResult {
      resultsBySite = Map.copyOf(resultsBySite);
    }

    private static QueryResult ofResultLine(ResultLine resultLine) {
      return QueryResult.builder()
              .resultsBySite(Map.of(resultLine.siteName(), resultLine))
              .build();
    }

    /**
     * Merges the result lines of {@code this} and the {@code other} query
     * result, returning a new query result.
     * <p>
     * Prioritises result lines of {@code this} query result over the ones of
     * the {@code other} query result in a way that result lines of a site
     * already present in {@code this} query result will not be overwritten with
     * ones from the {@code other} query result.
     *
     * @param other the query result to merge into this query result
     * @return a new query result containing the result lines from this query
     * result and result lines from new sites of the other query result
     */
    private QueryResult merge(QueryResult other) {
      Map<String, ResultLine> mergedResultsBySite = new HashMap<>(
          resultsBySite);
      other.resultsBySite.forEach(mergedResultsBySite::putIfAbsent);
      return QueryResult.builder()
              .resultsBySite(mergedResultsBySite)
              .build();
    }
  }
}
