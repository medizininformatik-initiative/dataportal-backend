package de.medizininformatikinitiative.dataportal.backend.query.broker;

/**
 * Indicates that a site was not found with regards to a specific query.
 */
public class SiteNotFoundException extends Exception {
  public SiteNotFoundException(String msg) {
    super(msg);
  }

  public SiteNotFoundException(String queryId, String siteId) {
    super("Site with ID '" + siteId + "' was not found with regards to query with ID '" + queryId + "'.");
  }
}
