package de.medizininformatikinitiative.dataportal.backend.query.broker.dsf;

import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.Certificate;

/**
 * An entity that can provide a security context for communicating with a FHIR server.
 */
public class DSFFhirSecurityContextProvider implements FhirSecurityContextProvider {

  private final String clientKeyFile;
  private final char[] clientKeyPassword;
  private final String trustCertificateFile;
  private String clientCertificateFile;

  public DSFFhirSecurityContextProvider(String clientCertificateFile, String clientKeyFile, char[] clientKeyPassword,
                                        String trustCertificateFile) {
    this.clientCertificateFile = clientCertificateFile;
    this.clientKeyFile = clientKeyFile;
    this.clientKeyPassword = clientKeyPassword;
    this.trustCertificateFile = trustCertificateFile;
  }

  @Override
  public FhirSecurityContext provideSecurityContext() throws FhirSecurityContextProvisionException {
    try {
      var localClientCertificateFile = Paths.get(clientCertificateFile);
      if (!Files.isReadable(localClientCertificateFile)) {
        throw new IOException("Client certificate file '" + localClientCertificateFile + "' not readable");
      }
      var localClientKeyFile = Paths.get(clientKeyFile);
      if (!Files.isReadable(localClientKeyFile)) {
        throw new IOException("Client key file '" + localClientKeyFile + "' not readable");
      }
      var localKeyStore = CertificateHelper.toJksKeyStore(
          PemIo.readPrivateKeyFromPem(localClientKeyFile, clientKeyPassword),
          new Certificate[]{PemIo.readX509CertificateFromPem(localClientCertificateFile)},
          "backend-cert", clientKeyPassword);
      var localTrustCertificateFile = Paths.get(trustCertificateFile);
      if (!Files.isReadable(localTrustCertificateFile)) {
        throw new IOException("Certificate file '" + trustCertificateFile + "' not readable");
      }
      var localTrustStore = CertificateReader.allFromCer(localTrustCertificateFile);


      return new FhirSecurityContext(localKeyStore, localTrustStore, clientKeyPassword);
    } catch (Exception e) {
      throw new FhirSecurityContextProvisionException(e);
    }
  }
}
