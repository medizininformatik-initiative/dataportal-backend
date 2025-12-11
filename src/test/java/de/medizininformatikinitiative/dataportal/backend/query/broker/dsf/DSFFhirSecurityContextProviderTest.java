package de.medizininformatikinitiative.dataportal.backend.query.broker.dsf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DSFFhirSecurityContextProviderTest {

  private static final String PASSWORD = "password";
  static private CertificateFactory cf;

  @BeforeAll
  static void setup() throws Exception {
    cf = CertificateFactory.getInstance("X.509");
  }

  @Test
  void trustStoreContainsCertificateFromPEM() throws Exception {
    var securityContextProvider = new DSFFhirSecurityContextProvider(
        getFilePath("client.crt"), getFilePath("client.key"), PASSWORD, getFilePath("foo.pem"));

    var securityContext = securityContextProvider.provideSecurityContext();

    assertThat(getCertificateAlias(securityContext, "foo.pem")).isNotBlank();
  }

  @Test
  void trustStoreContainsMultipleCertificatesFromPEM() throws Exception {
    DSFFhirSecurityContextProvider securityContextProvider = new DSFFhirSecurityContextProvider(
        getFilePath("client.crt"), getFilePath("client.key"), PASSWORD, getFilePath("multiple.pem"));

    FhirSecurityContext securityContext = securityContextProvider.provideSecurityContext();

    assertThat(getCertificateAlias(securityContext, "foo.pem")).isNotBlank();
    assertThat(getCertificateAlias(securityContext, "bar.pem")).isNotBlank();
  }

  @Test
  void keyStoreContainsClientCertificateAndKey() throws Exception {
    var securityContextProvider = new DSFFhirSecurityContextProvider(
        getFilePath("client.crt"), getFilePath("client.key"), PASSWORD, getFilePath("multiple.pem"));

    var securityContext = securityContextProvider.provideSecurityContext();

    var alias = assertThat(Collections.list(securityContext.getKeyStore().aliases()))
        .hasSize(1)
        .first()
        .actual();
    assertThat(securityContext.getKeyStore().getKey(alias, securityContext.getKeyStorePassword())).isNotNull();
  }

  @Test
  void failsOnMissingClientCertificate() throws Exception {
    var securityContextProvider = new DSFFhirSecurityContextProvider(
        "nonexisting.crt", getFilePath("client.key"), PASSWORD, getFilePath("foo.pem"));

    assertThatThrownBy(() -> securityContextProvider.provideSecurityContext()).cause()
        .hasMessageContainingAll("Client certificate file", "not readable");
  }

  @Test
  void failsOnMissingClientKey() throws Exception {
    var securityContextProvider = new DSFFhirSecurityContextProvider(
        getFilePath("client.crt"), "nonexisting.key", PASSWORD, getFilePath("foo.pem"));

    assertThatThrownBy(() -> securityContextProvider.provideSecurityContext()).cause()
        .hasMessageContainingAll("Client key file", "not readable");
  }

  @Test
  void failsOnMissingCaCertificate() throws Exception {
    var securityContextProvider = new DSFFhirSecurityContextProvider(
        getFilePath("client.crt"), getFilePath("client.key"), PASSWORD, "nonexisting.pem");

    assertThatThrownBy(() -> securityContextProvider.provideSecurityContext()).cause()
        .hasMessageContainingAll("Certificate file", "not readable");
  }

  @Test
  void failsOnWrongClientKeyPassword() throws Exception {
    var securityContextProvider = new DSFFhirSecurityContextProvider(
        getFilePath("client.crt"), getFilePath("client.key"), "WrongPassword",
        getFilePath("foo.pem"));

    assertThatThrownBy(() -> securityContextProvider.provideSecurityContext()).cause()
        .hasMessageContaining("unable to read encrypted data");
  }

  @Test
  void unencryptedClientKeyWithEmptyPassword() throws Exception {
    var securityContextProvider = new DSFFhirSecurityContextProvider(
        getFilePath("client.crt"), getFilePath("unencryptedClient.key"), "",
        getFilePath("foo.pem"));

    var securityContext = securityContextProvider.provideSecurityContext();

    assertThat(getCertificateAlias(securityContext, "foo.pem")).isNotBlank();
  }

  @Test
  void unencryptedClientKeyWithNullPassword() throws Exception {
    var securityContextProvider = new DSFFhirSecurityContextProvider(
        getFilePath("client.crt"), getFilePath("unencryptedClient.key"), null,
        getFilePath("foo.pem"));

    var securityContext = securityContextProvider.provideSecurityContext();

    assertThat(getCertificateAlias(securityContext, "foo.pem")).isNotBlank();
  }

  @Test
  void unencryptedClientKeyWithPassword() throws Exception {
    var securityContextProvider = new DSFFhirSecurityContextProvider(
        getFilePath("client.crt"), getFilePath("unencryptedClient.key"), "foobar",
        getFilePath("foo.pem"));

    var securityContext = securityContextProvider.provideSecurityContext();

    assertThat(getCertificateAlias(securityContext, "foo.pem")).isNotBlank();
  }

  private String getCertificateAlias(FhirSecurityContext securityContext, String fileName) throws Exception {
    InputStream resource = DSFFhirSecurityContextProviderTest.class.getResourceAsStream(fileName);
    Certificate cert = cf.generateCertificate(resource);
    return securityContext.trustStore.getCertificateAlias(cert);
  }

  private String getFilePath(String filename) {
    return DSFFhirSecurityContextProviderTest.class.getResource(filename).getPath();
  }
}
