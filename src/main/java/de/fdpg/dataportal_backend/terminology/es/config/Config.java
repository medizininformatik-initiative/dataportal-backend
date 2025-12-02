package de.fdpg.dataportal_backend.terminology.es.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
@ConditionalOnExpression("${app.elastic.enabled}")
public class Config extends ElasticsearchConfiguration {
  
  @Value("${app.elastic.host:localhost:9200}")
  private String elasticHost;
  
  @Override
  public ClientConfiguration clientConfiguration() {
    return ClientConfiguration.builder()
        .connectedTo(elasticHost)
        .build();
  }
}