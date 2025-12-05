package de.medizininformatikinitiative.dataportal.backend.query.ratelimiting;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.extra.PeriodDuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("query")
@Tag("ratelimiting")
@ExtendWith(MockitoExtension.class)
public class RateLimitingServiceTest {

  private final PeriodDuration intervalPollingSummary = PeriodDuration.of(Duration.ofSeconds(1));
  private final PeriodDuration intervalPollingDetailed = PeriodDuration.of(Duration.ofSeconds(1));
  private final int amountDetailedObfuscated = 2;
  private final PeriodDuration intervalDetailedObfuscated = PeriodDuration.of(Duration.ofSeconds(2));

  private RateLimitingService rateLimitingService;

  @BeforeEach
  void setUp() {
    this.rateLimitingService = new RateLimitingService(intervalPollingSummary, intervalPollingDetailed, amountDetailedObfuscated,
        intervalDetailedObfuscated);
  }

  @Test
  void testResolveBucket() {
    Bucket summaryBucketSomeone = rateLimitingService.resolveSummaryResultBucket("someone");
    assertNotNull(summaryBucketSomeone);
    Bucket summaryBucketSomeoneElse = rateLimitingService.resolveSummaryResultBucket("someone-else");
    assertNotNull(summaryBucketSomeoneElse);
    assertNotEquals(summaryBucketSomeone, summaryBucketSomeoneElse);
    assertEquals(summaryBucketSomeone, rateLimitingService.resolveSummaryResultBucket("someone"));

    Bucket detailedBucketSomeone = rateLimitingService.resolveSummaryResultBucket("someone");
    assertNotNull(detailedBucketSomeone);
    Bucket detailedBucketSomeoneElse = rateLimitingService.resolveSummaryResultBucket("someone-else");
    assertNotNull(detailedBucketSomeoneElse);
    assertNotEquals(detailedBucketSomeone, detailedBucketSomeoneElse);
    assertEquals(detailedBucketSomeone, rateLimitingService.resolveSummaryResultBucket("someone"));
  }

  @Test
  void testResolveBucketRefill() throws InterruptedException {
    Bucket bucketSomeoneSummary = rateLimitingService.resolveSummaryResultBucket("someone");
    assertTrue(bucketSomeoneSummary.tryConsume(1));
    assertFalse(bucketSomeoneSummary.tryConsume(1));
    Thread.sleep(TimeUnit.MILLISECONDS.convert(intervalPollingSummary.getDuration()));
    assertTrue(bucketSomeoneSummary.tryConsume(1));

    Bucket bucketSomeoneDetailed = rateLimitingService.resolveDetailedObfuscatedResultBucket("someone");
    assertTrue(bucketSomeoneDetailed.tryConsume(1));
    assertFalse(bucketSomeoneDetailed.tryConsume(1));
    Thread.sleep(TimeUnit.MILLISECONDS.convert(intervalPollingSummary.getDuration()));
    assertTrue(bucketSomeoneDetailed.tryConsume(1));
  }
}
