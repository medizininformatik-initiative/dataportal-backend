package de.fdpg.dataportal_backend.query.broker;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import lombok.NonNull;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.URI;

public final class OAuthInterceptor implements IClientInterceptor {

  private static final int TOKEN_EXPIRY_THRESHOLD = 5000;
  private HTTPRequest tokenRequest;
  private AccessToken token;
  private DateTime tokenExpiry;
  private Issuer issuer;
  private ClientSecretBasic clientAuth;

  public OAuthInterceptor(@NonNull String oauthIssuerUrl, @NonNull String oauthClientId,
                          @NonNull String oauthClientSecret) {
    clientAuth = new ClientSecretBasic(new ClientID(oauthClientId), new Secret(oauthClientSecret));
    issuer = new Issuer(oauthIssuerUrl);
  }

  public String getToken() {
    if (token == null || DateTime.now().plus(TOKEN_EXPIRY_THRESHOLD).isAfter(tokenExpiry)) {
      try {
        TokenResponse response = TokenResponse.parse(getTokenRequest().send());
        if (!response.indicatesSuccess()) {
          TokenErrorResponse errorResponse = response.toErrorResponse();
          throw new OAuthClientException(errorResponse.getErrorObject().getCode() + " - "
              + errorResponse.getErrorObject().getDescription());
        }
        AccessTokenResponse successResponse = response.toSuccessResponse();

        token = successResponse.getTokens().getAccessToken();
        tokenExpiry = DateTime.now().plus(token.getLifetime() * 1000);
      } catch (GeneralException | IOException e) {
        throw new OAuthClientException("Request for OAuth2 access token failed", e);
      }
    }
    return token.getValue();
  }

  private HTTPRequest getTokenRequest() throws GeneralException, IOException {
    if (tokenRequest == null) {
      tokenRequest = new TokenRequest.Builder(getTokenUri(), clientAuth, new ClientCredentialsGrant())
          .build().toHTTPRequest();
    }
    return tokenRequest;
  }

  private URI getTokenUri() throws GeneralException, IOException {
    return OIDCProviderMetadata.resolve(issuer).getTokenEndpointURI();
  }

  @Override
  public void interceptRequest(IHttpRequest theRequest) {
    theRequest.addHeader(Constants.HEADER_AUTHORIZATION,
        Constants.HEADER_AUTHORIZATION_VALPREFIX_BEARER + getToken());
  }

  @Override
  public void interceptResponse(IHttpResponse theResponse) throws IOException {
  }
}
