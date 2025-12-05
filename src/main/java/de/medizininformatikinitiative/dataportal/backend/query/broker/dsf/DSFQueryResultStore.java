package de.medizininformatikinitiative.dataportal.backend.query.broker.dsf;

import de.medizininformatikinitiative.dataportal.backend.query.broker.QueryNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.broker.SiteNotFoundException;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages {@link DSFQueryResult}s.
 */
class DSFQueryResultStore implements QueryResultStore {

  private final Map<String, Map<String, Integer>> results;

  public DSFQueryResultStore() {
    this.results = new HashMap<>();
  }


  @Override
  public void storeResult(@NotNull DSFQueryResult result) {
    Map<String, Integer> resultsPerClient = results.get(result.getQueryId());
    if (resultsPerClient == null) {
      resultsPerClient = new HashMap<>();
      resultsPerClient.put(result.getSiteId(), result.getMeasureCount());
      results.put(result.getQueryId(), resultsPerClient);
    } else {
      resultsPerClient.put(result.getSiteId(), result.getMeasureCount());
    }
  }

  @Override
  public int getMeasureCount(String queryId, String siteId) throws QueryNotFoundException, SiteNotFoundException {
    Map<String, Integer> resultsPerClient = results.get(queryId);
    if (resultsPerClient == null) {
      throw new QueryNotFoundException(queryId);
    }

    Integer count = resultsPerClient.get(siteId);
    if (count == null) {
      throw new SiteNotFoundException(queryId, siteId);
    }

    return count;
  }

  @Override
  public List<String> getSiteIdsWithResult(String queryId) throws QueryNotFoundException {
    Map<String, Integer> resultsPerClient = results.get(queryId);
    if (resultsPerClient == null) {
      throw new QueryNotFoundException(queryId);
    }

    return List.copyOf(resultsPerClient.keySet());
  }

  @Override
  public void removeResult(String queryId) throws QueryNotFoundException {
    Map<String, Integer> resultsPerClient = results.get(queryId);
    if (resultsPerClient == null) {
      throw new QueryNotFoundException(queryId);
    }

    results.remove(queryId);
  }
}
