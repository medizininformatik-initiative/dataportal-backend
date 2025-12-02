package de.fdpg.dataportal_backend.query.broker;

import de.fdpg.dataportal_backend.query.QueryMediaType;

/**
 * Indicates that a query does not contain the necessary query definition.
 */
public class QueryDefinitionNotFoundException extends Exception {

  public QueryDefinitionNotFoundException(String queryId) {
    super("Query with ID '" + queryId
        + "' does not contain any query definitions of a known type.");
  }

  public QueryDefinitionNotFoundException(String queryId, QueryMediaType queryMediaType) {
    super("Query with ID '" + queryId
        + "' does not contain a query definition for the mandatory type: " + queryMediaType);
  }
}
