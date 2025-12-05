package de.medizininformatikinitiative.dataportal.backend.query.broker.direct;

import de.medizininformatikinitiative.dataportal.backend.query.broker.BrokerClient;
import de.medizininformatikinitiative.dataportal.backend.query.broker.QueryDefinitionNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.broker.QueryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static de.medizininformatikinitiative.dataportal.backend.query.QueryMediaType.STRUCTURED_QUERY;
import static de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatus.COMPLETED;
import static de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatus.FAILED;

/**
 * A {@link BrokerClient} to be used to directly communicate with a Flare instance without the need
 * for using any middleware (Aktin or DSF).
 */
@Slf4j
public class DirectBrokerClientFlare extends DirectBrokerClient {
  private static final String FLARE_QUERY_ENDPOINT_URL = "/query/execute";
  private final WebClient webClient;

  /**
   * Creates a new {@link DirectBrokerClientFlare} instance that uses the given web client to communicate with a Flare
   * instance.
   *
   * @param webClient A web client to communicate with a Flare instance.
   */
  public DirectBrokerClientFlare(WebClient webClient, boolean obfuscateResultCount) {
    super(obfuscateResultCount);
    this.webClient = Objects.requireNonNull(webClient);
    listeners = new ArrayList<>();
    brokerQueries = new HashMap<>();
  }

  @Override
  public void publishQuery(String brokerQueryId) throws QueryNotFoundException, QueryDefinitionNotFoundException, IOException {
    var query = findQuery(brokerQueryId);
    var structuredQueryContent = query.getQueryDefinition(STRUCTURED_QUERY);

    try {
      webClient.post()
          .uri(FLARE_QUERY_ENDPOINT_URL)
          .header(HttpHeaders.CONTENT_TYPE, STRUCTURED_QUERY.getRepresentation())
          .bodyValue(structuredQueryContent)
          .retrieve()
          .bodyToMono(String.class)
          .map(Integer::valueOf)
          .doOnError(error -> {
            log.error(error.getMessage(), error);
            updateQueryStatus(query, FAILED);
          })
          .subscribe(val -> {
            query.setResult(obfuscateResultCount ? obfuscate(val) : val);
            updateQueryStatus(query, COMPLETED);
          });
    } catch (Exception e) {
      throw new IOException("An error occurred while publishing the query with ID: " + brokerQueryId, e);
    }
  }

}
