package de.medizininformatikinitiative.dataportal.backend.query.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import de.medizininformatikinitiative.dataportal.backend.query.broker.BrokerClient;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.QueryContentRepository;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.QueryDispatchRepository;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.QueryRepository;
import de.medizininformatikinitiative.dataportal.backend.query.translation.QueryTranslationComponent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class QueryDispatchSpringConfig {

  // Keep the dispatcher a singleton instance since it uses a task executor.
  // Without this you may use more threads than intended.
  @Bean
  public QueryDispatcher createQueryDispatcher(
      @Qualifier("brokerClients") List<BrokerClient> queryBrokerClients,
      QueryTranslationComponent queryTranslationComponent,
      QueryHashCalculator queryHashCalculator,
      @Qualifier("translation") ObjectMapper jsonUtil,
      QueryRepository queryRepository,
      QueryContentRepository queryContentRepository,
      QueryDispatchRepository queryDispatchRepository) {
    return new QueryDispatcher(queryBrokerClients, queryTranslationComponent, queryHashCalculator, jsonUtil, queryRepository,
        queryContentRepository, queryDispatchRepository);
  }

  @Bean
  public QueryHashCalculator createQueryHashCalculator() {
    return new QueryHashCalculator(Hashing.sha256());
  }
}
