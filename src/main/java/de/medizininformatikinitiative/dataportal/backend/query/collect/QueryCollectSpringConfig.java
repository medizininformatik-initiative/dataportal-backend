package de.medizininformatikinitiative.dataportal.backend.query.collect;

import de.medizininformatikinitiative.dataportal.backend.query.persistence.QueryRepository;
import de.medizininformatikinitiative.dataportal.backend.query.result.ResultService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryCollectSpringConfig {

  @Bean
  public QueryStatusListener createQueryStatusListener(QueryRepository queryRepository,
                                                       ResultService resultService) {
    return new QueryStatusListenerImpl(queryRepository, resultService);
  }
}
