package de.medizininformatikinitiative.dataportal.backend.query.broker.dsf;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.security.KeyStore;

/**
 * Holds information about how to securely communicate with a FHIR server.
 */
@Data
@AllArgsConstructor
class FhirSecurityContext {
  KeyStore keyStore;
  KeyStore trustStore;
  private char[] keyStorePassword;
}
