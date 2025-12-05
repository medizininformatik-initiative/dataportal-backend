package de.medizininformatikinitiative.dataportal.backend.query.broker;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;

import java.io.IOException;

public final class NoOpInterceptor implements IClientInterceptor {
  @Override
  public void interceptResponse(IHttpResponse theResponse) throws IOException {
  }

  @Override
  public void interceptRequest(IHttpRequest theRequest) {
  }
}
