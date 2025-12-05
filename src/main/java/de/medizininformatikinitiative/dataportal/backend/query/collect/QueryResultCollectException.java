package de.medizininformatikinitiative.dataportal.backend.query.collect;

/**
 * Thrown when a collect related action fails.
 */
class QueryResultCollectException extends Exception {

  /**
   * Constructs a new {@link QueryResultCollectException} with the specified detail message.
   *
   * @param message The detail message.
   */
  QueryResultCollectException(String message) {
    super(message);
  }

  /**
   * Constructs a new {@link QueryResultCollectException} with the specified detail message and cause.
   *
   * @param message The detail message.
   * @param cause   The cause.
   */
  QueryResultCollectException(String message, Throwable cause) {
    super(message, cause);
  }
}
