package de.medizininformatikinitiative.dataportal.backend.query.collect;

/**
 * Represents an entity capable of receiving results from brokers for broker specific queries that they run.
 */
public interface QueryStatusListener {

  /**
   * Processes update notifications from brokers to one of their broker specific queries that is associated with an
   * internal (backend specific) query.
   *
   * @param backendQueryId    Identifier for a backend specific query that the status update is associated with.
   * @param queryStatusUpdate Describes the update for a broker specific query.
   */
  void onClientUpdate(Long backendQueryId, QueryStatusUpdate queryStatusUpdate);
}
