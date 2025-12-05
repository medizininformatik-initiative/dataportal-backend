package de.medizininformatikinitiative.dataportal.backend.query.broker;

public class OAuthClientException extends RuntimeException {

  private static final long serialVersionUID = -5840162115734733430L;

  public OAuthClientException(String message) {
    super(message);
  }

  public OAuthClientException(String message, Exception cause) {
    super(message, cause);
  }

}
