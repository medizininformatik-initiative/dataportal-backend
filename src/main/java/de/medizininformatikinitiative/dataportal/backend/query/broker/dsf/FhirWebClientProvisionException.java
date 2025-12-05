package de.medizininformatikinitiative.dataportal.backend.query.broker.dsf;

/**
 * Indicates that a FHIR web client could not be provisioned.
 */
public class FhirWebClientProvisionException extends Exception {
  public FhirWebClientProvisionException(Throwable cause) {
    super(cause);
  }
}
