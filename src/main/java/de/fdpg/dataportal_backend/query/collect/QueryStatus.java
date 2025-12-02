package de.fdpg.dataportal_backend.query.collect;

/**
 * Represents different states a query can be in.
 */
public enum QueryStatus {
  RETRIEVED,
  QUEUED,
  EXECUTING,
  COMPLETED,
  FAILED
}
