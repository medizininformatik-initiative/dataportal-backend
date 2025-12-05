package de.medizininformatikinitiative.dataportal.backend.query.broker.dsf;

import de.medizininformatikinitiative.dataportal.backend.query.broker.QueryNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.broker.SiteNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatusListener;

import java.io.IOException;
import java.util.List;

/**
 * Represents an entity capable of collecting query results and notifying interested parties about them.
 */
interface QueryResultCollector {

  /**
   * Registers a listener that gets called whenever a new query result comes in.
   *
   * @param broker   The broker that acts as a source for new results.
   * @param listener The listener that gets notified for new results.
   * @throws IOException If there is a problem when trying to establish a result listener channel.
   */
  void addResultListener(DSFBrokerClient broker, QueryStatusListener listener) throws IOException;

  /**
   * Gets the feasibility of a specific site within a query.
   *
   * @param queryId Identifies the associated query.
   * @param siteId  Identifies the site within the query.
   * @return Site's feasibility within the query.
   * @throws QueryNotFoundException If the given query ID does not identify a known query.
   * @throws SiteNotFoundException  If the given site ID does not identify a known site within the query results.
   */
  int getResultFeasibility(String queryId, String siteId) throws QueryNotFoundException, SiteNotFoundException;

  /**
   * Gets all site identifiers of a specific query who already published a query result.
   *
   * @param queryId Identifies the associated query.
   * @return All site identifiers who already published a query result.
   * @throws QueryNotFoundException If the given query ID does not identify a known query.
   */
  List<String> getResultSiteIds(String queryId) throws QueryNotFoundException;

  /**
   * Removes all results associated with a specific query if there are any.
   *
   * @param queryId Identifies the associated query.
   * @throws QueryNotFoundException If the given query ID does not identify a known query.
   */
  void removeResults(String queryId) throws QueryNotFoundException;
}
