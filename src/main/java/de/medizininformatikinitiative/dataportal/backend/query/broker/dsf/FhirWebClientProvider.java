package de.medizininformatikinitiative.dataportal.backend.query.broker.dsf;

import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.client.WebsocketClient;

/**
 * Represents an entity capable of providing different kinds of FHIR web clients. Supported web client types are
 * webservice (HTTP) and websocket.
 */
public interface FhirWebClientProvider {

  /**
   * Provides a {@link FhirWebserviceClient} to communicate with a FHIR server via HTTP.
   *
   * @return A {@link FhirWebserviceClient}.
   * @throws FhirWebClientProvisionException If the webservice client can not be provisioned.
   */
  FhirWebserviceClient provideFhirWebserviceClient() throws FhirWebClientProvisionException;

  /**
   * Provides a {@link WebsocketClient} to communicate with a FHIR server via a websocket.
   *
   * @param reconnector A {@link Runnable} that can be used to trigger a reconnection in case the websocket connection
   *          gets disconnected abnormally
   * @return A {@link WebsocketClient}.
   * @throws FhirWebClientProvisionException If the websocket client can not be provisioned.
   */
  WebsocketClient provideFhirWebsocketClient(Runnable reconnector) throws FhirWebClientProvisionException;

}
