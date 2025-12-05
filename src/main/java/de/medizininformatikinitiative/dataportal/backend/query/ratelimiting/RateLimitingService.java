package de.medizininformatikinitiative.dataportal.backend.query.ratelimiting;

import io.github.bucket4j.Bucket;
import lombok.Getter;
import org.threeten.extra.PeriodDuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implement a rate-limiting service via a {@link Bucket} on a per-user basis.
 * <p>
 * The limit is defined via application.yaml or env variable.
 */
public class RateLimitingService {

  private final Map<String, Bucket> summaryResultRetrievalCache = new ConcurrentHashMap<>();
  private final Map<String, Bucket> detailedResultRetrievalCache = new ConcurrentHashMap<>();
  private final Map<String, Bucket> detailedViewObfuscatedResultRetrievalCache = new ConcurrentHashMap<>();

  private final PeriodDuration intervalPollingSummary;
  private final PeriodDuration intervalPollingDetailed;
  private final PeriodDuration intervalDetailedObfuscated;
  @Getter
  private final int amountDetailedObfuscated;

  /**
   * Creates a new RateLimitingService.
   * <p>
   * This will limit the polling rate for default users on any result endpoint. It will also limit
   * the amount of times a user can request detailed obfuscated results in a given timespan.
   *
   * @param intervalPollingSummary     the duration after which the user can poll summary results again
   * @param intervalPollingDetailed    the duration after which the user can poll detailed results again
   * @param amountDetailedObfuscated   the amount of times a user can request detailed obfuscated results
   * @param intervalDetailedObfuscated the timespan after which a users access is "forgotten"
   */
  public RateLimitingService(PeriodDuration intervalPollingSummary, PeriodDuration intervalPollingDetailed, int amountDetailedObfuscated, PeriodDuration intervalDetailedObfuscated) {
    this.intervalPollingSummary = intervalPollingSummary;
    this.intervalPollingDetailed = intervalPollingDetailed;
    this.amountDetailedObfuscated = amountDetailedObfuscated;
    this.intervalDetailedObfuscated = intervalDetailedObfuscated;
  }

  public Bucket resolveSummaryResultBucket(String userId) {
    return summaryResultRetrievalCache.computeIfAbsent(userId, this::newSummaryResultBucket);
  }

  public Bucket resolveDetailedObfuscatedResultBucket(String userId) {
    return detailedResultRetrievalCache.computeIfAbsent(userId, this::newDetailedResultBucket);
  }

  public Bucket resolveViewDetailedObfuscatedBucket(String userId) {
    return detailedViewObfuscatedResultRetrievalCache.computeIfAbsent(userId, this::newViewDetailedObfuscatedResultBucket);
  }

  public void addTokensToDetailedObfuscatedResultBucket(String userId, int amount) {
    resolveViewDetailedObfuscatedBucket(userId).addTokens(amount);
  }

  private Bucket newSummaryResultBucket(String userId) {
    return Bucket.builder()
        .addLimit(limit -> limit.capacity(1).refillIntervally(1, intervalPollingSummary.getDuration()))
        .build();
  }

  private Bucket newDetailedResultBucket(String userId) {
    return Bucket.builder()
        .addLimit(limit -> limit.capacity(1).refillIntervally(1, intervalPollingDetailed.getDuration()))
        .build();
  }

  private Bucket newViewDetailedObfuscatedResultBucket(String userId) {
    return Bucket.builder()
        .addLimit(limit -> limit.capacity(amountDetailedObfuscated).refillIntervally(1, intervalDetailedObfuscated.getDuration()))
        .build();
  }

}
