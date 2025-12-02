package de.fdpg.dataportal_backend.query.broker.dsf;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Holds information about an optional proxy used to communicate with a FHIR server.
 */
@Data
@AllArgsConstructor
public class FhirProxyContext {
  String proxyHost;
  String username;
  String password;

  public char[] getPassword() {
    return (password == null) ? null : password.toCharArray();
  }
}
