package de.medizininformatikinitiative.dataportal.backend.query.result;

import de.medizininformatikinitiative.dataportal.backend.query.persistence.QueryDispatchRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aktin.broker.client2.AuthFilter;
import org.aktin.broker.client2.BrokerAdmin2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.threeten.extra.PeriodDuration;

import java.net.URI;
import java.net.http.WebSocket.Builder;

@Configuration
@Slf4j
public class ResultServiceSpringConfig {

  @Value("${app.broker.aktin.enabled:false}")
  private boolean aktinEnabled;

  @Value("${app.broker.aktin.broker.baseUrl:}")
  private String brokerBaseUrl;

  @Value("${app.broker.aktin.broker.apiKey:}")
  private String brokerApiKey;

  @Bean
  public ResultService createResultService(
      @Value("${app.queryResultExpiry}") String resultExpiry, QueryDispatchRepository queryDispatchRepository) {

    BrokerAdmin2 client = null;

    if (aktinEnabled) {
      client = new BrokerAdmin2(URI.create(brokerBaseUrl));
      client.setAuthFilter(new ApiKeyAuthFilter(brokerApiKey));
    }

    log.info("Create ResultService with result TTL of {}", resultExpiry);
    return new ResultService(PeriodDuration.parse(resultExpiry), client, queryDispatchRepository);
  }

  @AllArgsConstructor
  private static class ApiKeyAuthFilter implements AuthFilter {

    final private String key;

    @Override
    public void addAuthentication(Builder builder) {
      builder.header("Authorization", "Bearer " + key);
    }

    @Override
    public void addAuthentication(java.net.http.HttpRequest.Builder builder) {
      builder.header("Authorization", "Bearer " + key);
    }
  }
}
