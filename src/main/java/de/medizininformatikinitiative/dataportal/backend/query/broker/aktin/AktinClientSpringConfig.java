package de.medizininformatikinitiative.dataportal.backend.query.broker.aktin;

import de.medizininformatikinitiative.dataportal.backend.query.broker.BrokerClient;
import lombok.AllArgsConstructor;
import org.aktin.broker.client2.AuthFilter;
import org.aktin.broker.client2.BrokerAdmin2;
import org.aktin.broker.client2.ReconnectingListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.net.URI;
import java.net.http.WebSocket.Builder;


/**
 * Spring configuration for providing a {@link AktinBrokerClient} instance.
 */
@Lazy
@Configuration
public class AktinClientSpringConfig {

  @Value("${app.broker.aktin.broker.baseUrl:}")
  private String brokerBaseUrl;

  @Value("${app.broker.aktin.broker.apiKey:}")
  private String brokerApiKey;

  @Qualifier("aktin")
  @Bean
  public BrokerClient aktinBrokerClient() {
    BrokerAdmin2 client = new BrokerAdmin2(URI.create(brokerBaseUrl));
    // when websocket is disconnected, automatically reconnect. delay 10 seconds between failures.
    client.addListener(ReconnectingListener.forAdmin(client, 10 * 1000, -1));
    client.setAuthFilter(new ApiKeyAuthFilter(brokerApiKey));
    return new AktinBrokerClient(client);
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
