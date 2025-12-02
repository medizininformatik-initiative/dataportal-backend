package de.fdpg.dataportal_backend.query.broker.dsf;

/**
 * Indicates that a FHIR security context could not be provisioned.
 */
public class FhirSecurityContextProvisionException extends Exception {
    public FhirSecurityContextProvisionException(Throwable cause) {
        super(cause);
    }

    public FhirSecurityContextProvisionException(String message) {
        super(message);
    }
}
