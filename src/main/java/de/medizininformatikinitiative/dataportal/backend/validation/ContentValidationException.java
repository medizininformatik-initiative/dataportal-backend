package de.medizininformatikinitiative.dataportal.backend.validation;

public class ContentValidationException extends RuntimeException {
  public ContentValidationException(Exception e) {
    super(e);
  }
}
