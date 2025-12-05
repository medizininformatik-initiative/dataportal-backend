package de.medizininformatikinitiative.dataportal.backend.query.dispatch;

/**
 * Thrown when a dispatch related action fails.
 */
public class QueryDispatchException extends Exception {

  /**
   * Constructs a new {@link QueryDispatchException} with the specified detail message.
   *
   * @param message The detail message.
   */
  public QueryDispatchException(String message) {
    super(message);
  }

  /**
   * Constructs a new {@link QueryDispatchException} with the specified detail message and cause.
   *
   * @param message The detail message.
   * @param cause   The cause.
   */
  public QueryDispatchException(String message, Throwable cause) {
    super(message, cause);
  }

}
