package de.fdpg.dataportal_backend.query.ratelimiting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.threeten.extra.PeriodDuration;

@Configuration
@Slf4j
public class RateLimitingServiceSpringConfig {

  @Bean
  public RateLimitingService createRateLimitingService(
      @Value("${app.privacy.quota.read.resultSummary.pollingInterval}") String pollingIntervalSummary,
      @Value("${app.privacy.quota.read.resultDetailedObfuscated.pollingInterval}") String pollingIntervalDetailed,
      @Value("${app.privacy.quota.read.resultDetailedObfuscated.amount}") int detailedObfuscatedAmount,
      @Value("${app.privacy.quota.read.resultDetailedObfuscated.interval}") String detailedObfuscatedInterval) {

    log.info(
        "Create RateLimitingService with interval of {} for summary result endpoint, {} for detailed"
            + " obfuscated results and {} allowed requests to detailed obfuscated result per {}",
        pollingIntervalSummary, pollingIntervalDetailed, detailedObfuscatedAmount, detailedObfuscatedInterval);
    return new RateLimitingService(PeriodDuration.parse(pollingIntervalSummary),
        PeriodDuration.parse(pollingIntervalDetailed),
        detailedObfuscatedAmount, PeriodDuration.parse(detailedObfuscatedInterval));
  }
}
