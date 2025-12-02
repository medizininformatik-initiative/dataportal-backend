package de.fdpg.dataportal_backend.query.broker;

import ca.uhn.fhir.context.FhirContext;
import de.fdpg.dataportal_backend.query.broker.mock.MockBrokerClient;
import de.fdpg.dataportal_backend.query.collect.QueryStatusListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Lazy;

@Configuration
@Slf4j
public class BrokerSpringConfig {

  @Value("${app.broker.mock.enabled:false}")
  private boolean mockClientEnabled;

  @Value("${app.broker.direct.enabled:false}")
  private boolean directClientEnabled;

  @Value("${app.broker.aktin.enabled:false}")
  private boolean aktinClientEnabled;

  @Value("${app.broker.dsf.enabled:false}")
  private boolean dsfClientEnabled;

  private final ApplicationContext ctx;

  public BrokerSpringConfig(ApplicationContext ctx) {
    this.ctx = ctx;
  }

  // Do NOT remove the qualifier annotation, since spring attempts to initialize ALL broker clients
  // and does not call this method anymore - rendering the enable-switches moot.

  @Bean
  @Lazy
  FhirContext fhirContext() {
    return FhirContext.forR4();
  }

  @Qualifier("brokerClients")
  @Bean
  public List<BrokerClient> createBrokerClients(QueryStatusListener statusListener) throws IOException {
    List<BrokerClient> brokerClients = new ArrayList<>();
    if (mockClientEnabled) {
      log.info("Enable mock client");
      brokerClients.add(new MockBrokerClient());
    }
    if (directClientEnabled) {
      log.info("Enable direct client");
      brokerClients.add(BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "direct"));
    }
    if (aktinClientEnabled) {
      log.info("Enable aktin client");
      brokerClients.add(BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "aktin"));
    }
    if (dsfClientEnabled) {
      log.info("Enable dsf client");
      brokerClients.add(BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "dsf"));
    }

    for (BrokerClient brokerClient : brokerClients) {
      brokerClient.addQueryStatusListener(statusListener);
    }

    return brokerClients;
  }
}
