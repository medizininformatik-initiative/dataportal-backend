package de.fdpg.dataportal_backend.query.collect;

import de.fdpg.dataportal_backend.query.persistence.QueryRepository;
import de.fdpg.dataportal_backend.query.result.ResultService;
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
