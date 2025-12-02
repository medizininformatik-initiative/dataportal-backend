package de.fdpg.dataportal_backend.query.broker;

import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Duration;

import static ca.uhn.fhir.rest.api.Constants.HEADER_AUTHORIZATION;
import static ca.uhn.fhir.rest.api.Constants.HEADER_AUTHORIZATION_VALPREFIX_BEARER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class OAuthInterceptorIT {

  @Container
  public static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:25.0")
      .withAdminUsername("admin")
      .withAdminPassword("admin")
      .withRealmImportFile(
          new ClassPathResource("realm-test-short-expiry.json", OAuthInterceptorIT.class).getPath())
      .withReuse(true);
  @Mock
  private IHttpRequest request;
  @Mock
  private IHttpResponse response;

  @Test
  public void getToken() {
    String issuerUrl = "http://" + keycloak.getHost() + ":" + keycloak.getFirstMappedPort() + "/realms/test";
    OAuthInterceptor interceptor = new OAuthInterceptor(issuerUrl, "account", "test");

    String token = interceptor.getToken();

    assertThat(token).isNotNull();
  }

  @Test
  public void authorizationHeaderIsSetInRequest() {
    String issuerUrl = "http://" + keycloak.getHost() + ":" + keycloak.getFirstMappedPort() + "/realms/test";
    OAuthInterceptor interceptor = new OAuthInterceptor(issuerUrl, "account", "test");

    interceptor.interceptRequest(request);
    String token = interceptor.getToken();

    verify(request).addHeader(HEADER_AUTHORIZATION, HEADER_AUTHORIZATION_VALPREFIX_BEARER + token);
  }

  @Test
  public void responseIsUnaltered() throws IOException {
    OAuthInterceptor interceptor = new OAuthInterceptor("http://foo.bar", "foo", "bar");

    interceptor.interceptResponse(response);

    verifyNoInteractions(response);
  }

  @Test
  public void tokenIsTheSameBeforeRefreshTimeout() throws InterruptedException {
    String issuerUrl = "http://" + keycloak.getHost() + ":" + keycloak.getFirstMappedPort() + "/realms/test";
    OAuthInterceptor interceptor = new OAuthInterceptor(issuerUrl, "account", "test");

    String token1 = interceptor.getToken();
    Thread.sleep(Duration.ofSeconds(2).toMillis());
    String token2 = interceptor.getToken();

    assertThat(token1).isEqualTo(token2);
  }

  @Test
  public void tokenIsDifferentAfterRefreshTimeout() throws InterruptedException {
    String issuerUrl = "http://" + keycloak.getHost() + ":" + keycloak.getFirstMappedPort() + "/realms/test";
    OAuthInterceptor interceptor = new OAuthInterceptor(issuerUrl, "account", "test");

    String token1 = interceptor.getToken();
    Thread.sleep(Duration.ofSeconds(10).toMillis());
    String token2 = interceptor.getToken();

    assertThat(token1).isNotEqualTo(token2);
  }

  @Test
  public void errorWhenIssuerUrlIsNull() throws InterruptedException {
    assertThatThrownBy(() -> new OAuthInterceptor(null, "foo", "bar")).isInstanceOf(NullPointerException.class)
        .hasMessageContaining("oauthIssuerUrl");
  }

  @Test
  public void errorWhenClientIdIsNull() throws InterruptedException {
    assertThatThrownBy(() -> new OAuthInterceptor("http://foo.bar", null, "bar")).isInstanceOf(NullPointerException.class)
        .hasMessageContaining("oauthClientId");
  }

  @Test
  public void errorWhenClientSecretIsNull() throws InterruptedException {
    assertThatThrownBy(() -> new OAuthInterceptor("http://foo.bar", "foo", null)).isInstanceOf(NullPointerException.class)
        .hasMessageContaining("oauthClientSecret");
  }

  @Test
  public void errorWhenIssuerUrlIsWrong() throws InterruptedException {
    String host = "non.existing.url";
    String issuerUrl = "http://" + host + "/foo/bar";
    OAuthInterceptor interceptor = new OAuthInterceptor(issuerUrl, "foo", "bar");

    assertThatThrownBy(interceptor::getToken).isInstanceOf(OAuthClientException.class)
        .hasMessageContaining("Request for OAuth2 access token failed")
        .has(new Condition<Throwable>(s -> s.getCause() != null && s.getCause().getMessage().contains(host),
            "hostname in error message of cause"));
  }

  @Test
  public void errorWhenClientIdIsUnknown() throws InterruptedException {
    String issuerUrl = "http://" + keycloak.getHost() + ":" + keycloak.getFirstMappedPort() + "/realms/test";
    OAuthInterceptor interceptor = new OAuthInterceptor(issuerUrl, "foo", "test");

    assertThatThrownBy(interceptor::getToken).isInstanceOf(OAuthClientException.class)
        .hasMessageContaining("invalid_client");
  }

  @Test
  public void errorWhenClientSecretIsWrong() throws InterruptedException {
    String issuerUrl = "http://" + keycloak.getHost() + ":" + keycloak.getFirstMappedPort() + "/realms/test";
    OAuthInterceptor interceptor = new OAuthInterceptor(issuerUrl, "account", "foo");

    assertThatThrownBy(interceptor::getToken).isInstanceOf(OAuthClientException.class)
        .hasMessageContaining("unauthorized_client");
  }
}
