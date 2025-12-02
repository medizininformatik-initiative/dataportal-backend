package de.fdpg.dataportal_backend.query.broker.dsf;

import de.fdpg.dataportal_backend.query.broker.QueryNotFoundException;
import de.fdpg.dataportal_backend.query.broker.SiteNotFoundException;

import java.util.List;

/**
 * Represents a storage facility for {@link DSFQueryResult}s.
 */
public interface QueryResultStore {

  /**
   * Stores a result within the store.
   *
   * @param result The result that shall be stored.
   */
  void storeResult(DSFQueryResult result);

  /**
   * Gets the measure count from the store for a specific site associated with a query.
   *
   * @param queryId Identifies the query that the measure count is related to.
   * @param siteId  Identifies the site within the query that the measure count is related to.
   * @return The measure count.
   * @throws QueryNotFoundException If the given query ID does not identify a known query.
   * @throws SiteNotFoundException  If the given site ID does not identify a known site.
   */
  int getMeasureCount(String queryId, String siteId) throws QueryNotFoundException, SiteNotFoundException;

  /**
   * Gets all site IDs from the store that are associated with a specific query.
   * <p>
   * This will only return site IDs that already have a result (measure count) associated with them.
   *
   * @param queryId Identifies the query that the sites are related to.
   * @return List of all site IDs associated with the query that already have a result.
   * @throws QueryNotFoundException If the given query ID does not identify a known query.
   */
  List<String> getSiteIdsWithResult(String queryId) throws QueryNotFoundException;

  /**
   * Removes a result from the store.
   *
   * @param queryId Identifies the query whose associated results shall be removed.
   * @throws QueryNotFoundException If the given query ID does not identify a known query.
   */
  void removeResult(String queryId) throws QueryNotFoundException;
}
