package de.medizininformatikinitiative.dataportal.backend.query.broker.direct;

import de.medizininformatikinitiative.dataportal.backend.query.QueryMediaType;
import de.medizininformatikinitiative.dataportal.backend.query.broker.BrokerClient;
import de.medizininformatikinitiative.dataportal.backend.query.broker.QueryDefinitionNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.broker.QueryNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.broker.SiteNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatus;
import de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatusListener;
import de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatusUpdate;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.BrokerClientType;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static de.medizininformatikinitiative.dataportal.backend.query.persistence.BrokerClientType.DIRECT;

public abstract class DirectBrokerClient implements BrokerClient {

  static final String SITE_ID_LOCAL = "1";

  private static final String SITE_NAME_LOCAL = "Local Server";

  protected List<QueryStatusListener> listeners;
  protected Map<String, DirectQuery> brokerQueries;


  protected boolean obfuscateResultCount;

  protected DirectBrokerClient(boolean obfuscateResultCount) {
    this.obfuscateResultCount = obfuscateResultCount;
  }

  @Override
  public final BrokerClientType getBrokerType() {
    return DIRECT;
  }

  @Override
  public void addQueryStatusListener(QueryStatusListener queryStatusListener) {
    listeners.add(queryStatusListener);
  }

  @Override
  public String createQuery(Long backendQueryId) {
    var brokerQuery = DirectQuery.create(backendQueryId);
    var brokerQueryId = brokerQuery.getQueryId();
    brokerQueries.put(brokerQueryId, brokerQuery);

    return brokerQueryId;
  }

  @Override
  public void addQueryDefinition(String brokerQueryId, QueryMediaType queryMediaType, String content) throws QueryNotFoundException {
    findQuery(brokerQueryId).addQueryDefinition(queryMediaType, content);
  }

  @Override
  public void closeQuery(String brokerQueryId) throws QueryNotFoundException {
    if (brokerQueries.remove(brokerQueryId) == null) {
      throw new QueryNotFoundException(brokerQueryId);
    }
  }

  @Override
  public int getResultFeasibility(String brokerQueryId, String siteId) throws QueryNotFoundException, SiteNotFoundException {
    return findQuery(brokerQueryId).getResult();
  }

  @Override
  public List<String> getResultSiteIds(String brokerQueryId) throws QueryNotFoundException {
    return findQuery(brokerQueryId).hasResult() ? Collections.singletonList(SITE_ID_LOCAL) : Collections.emptyList();
  }

  @Override
  public String getSiteName(String siteId) throws SiteNotFoundException {
    if (SITE_ID_LOCAL.equals(siteId)) {
      return SITE_NAME_LOCAL;
    } else {
      throw new SiteNotFoundException("No site with id " + siteId + " found.");
    }
  }


  /**
   * Searches for a {@link DirectQuery} within this broker. The specified query ID identifies the
   * query.
   *
   * @param queryId The identifier of the query.
   * @return The query if there is any.
   * @throws QueryNotFoundException If the ID does not identify a known query.
   */
  protected DirectQuery findQuery(String queryId) throws QueryNotFoundException {
    return Optional.ofNullable(brokerQueries.get(queryId))
        .orElseThrow(() -> new QueryNotFoundException(queryId));
  }

  /**
   * "Obfuscates" a number by adding or subtracting a random number <=5.
   *
   * @param resultCount the precise result
   * @return The obfuscated result, or 0 if the obfuscated result is < 5
   */
  protected int obfuscate(int resultCount) {
    int obfuscatedResultCount =
        resultCount + ThreadLocalRandom.current().nextInt(11) - 5;
    if (obfuscatedResultCount < 5) {
      return 0;
    } else {
      return obfuscatedResultCount;
    }
  }

  /**
   * Updates a query status in all registered listeners.
   *
   * @param query       the query to update
   * @param queryStatus the {@link QueryStatus} to publish to the listeners
   */
  protected void updateQueryStatus(DirectQuery query, QueryStatus queryStatus) {
    var statusUpdate = QueryStatusUpdate.builder()
        .source(this)
        .brokerQueryId(query.getQueryId())
        .brokerSiteId(SITE_ID_LOCAL)
        .status(queryStatus)
        .build();
    listeners.forEach(
        l -> l.onClientUpdate(query.getBackendQueryId(), statusUpdate)
    );
  }

  /**
   * A data container representing a query used for direct communications with a Flare instance.
   */
  public static class DirectQuery {

    @Getter
    private final String queryId;
    @Getter
    private final Long backendQueryId;
    private final Map<QueryMediaType, String> queryDefinitions;
    @Setter
    private Integer result;

    private DirectQuery(String queryId, Long backendQueryId) {
      this.queryId = queryId;
      this.backendQueryId = backendQueryId;
      queryDefinitions = new EnumMap<>(QueryMediaType.class);
    }

    /**
     * Creates a new {@link DirectQuery} with a random UUID as a query ID.
     *
     * @param backendQueryId The query id in the backend
     * @return The created query.
     */
    public static DirectQuery create(Long backendQueryId) {
      return new DirectQuery(UUID.randomUUID().toString(), backendQueryId);
    }

    /**
     * Adds a query definition. A query definition is a query in a specific format (i.e. structured
     * query / CQL). The specified mime type defines the format of the query itself. When invoked
     * multiple times for a single mime type, any already existing query content associated with
     * this mime type gets overwritten.
     *
     * @param queryMediaType The {@link QueryMediaType} defining the format of the query.
     * @param content        The actual query in its string representation.
     */
    public void addQueryDefinition(QueryMediaType queryMediaType, String content) {
      queryDefinitions.put(queryMediaType, content);
    }

    /**
     * Gets a single query definition associated with the given mime type.
     *
     * @param queryMediaType The {@link QueryMediaType} of the query.
     * @return The query in its string representation or null if there is no query definition
     * associated with the specified mime type.
     */
    public String getQueryDefinition(QueryMediaType queryMediaType)
        throws QueryDefinitionNotFoundException {
      return Optional.ofNullable(queryDefinitions.get(queryMediaType))
          .orElseThrow(() -> new QueryDefinitionNotFoundException(queryId, queryMediaType)
          );
    }

    /**
     * Gets the feasibility result of the local site.
     *
     * @return The feasibility result of the local site identified by the specified identifier.
     * @throws SiteNotFoundException If the ID does not identify a known site.
     */
    public int getResult() throws SiteNotFoundException {
      return Optional.ofNullable(result)
          .orElseThrow(() -> new SiteNotFoundException(queryId, SITE_ID_LOCAL));
    }

    /**
     * Returns whether the local site has a reported result.
     *
     * @return true if a result is available, false otherwise
     */
    public boolean hasResult() {
      return result != null;
    }
  }
}
