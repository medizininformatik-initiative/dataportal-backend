package de.fdpg.dataportal_backend.query.broker.dsf;

/**
 * Represents an entity capable of providing a security context for a FHIR web client.
 */
public interface FhirSecurityContextProvider {

    /**
     * Provides a {@link FhirSecurityContext} to be used when communicating with a FHIR server.
     *
     * @return A {@link FhirSecurityContext}.
     * @throws FhirSecurityContextProvisionException If the security context can not be provisioned.
     */
    FhirSecurityContext provideSecurityContext() throws FhirSecurityContextProvisionException;
}
