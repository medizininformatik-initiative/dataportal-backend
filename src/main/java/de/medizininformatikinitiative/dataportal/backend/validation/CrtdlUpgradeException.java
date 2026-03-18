package de.medizininformatikinitiative.dataportal.backend.validation;

public class CrtdlUpgradeException extends RuntimeException {
  public CrtdlUpgradeException(String message) {
    super(message);
  }

  public CrtdlUpgradeException(Exception e) {
    super(e);
  }
}
