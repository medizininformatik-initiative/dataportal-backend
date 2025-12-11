package de.medizininformatikinitiative.dataportal.backend.settings;

import de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = "Location")
public class SettingsController {

  private static final String KEY_DSE_URL = "passthroughDsePatientProfileUrl";
  private static final String KEY_READ_RESULT_DETAILED_OBFUSCATED_POLLING_INTERVAL = "readResultDetailedObfuscatedPollingInterval";
  private static final String KEY_READ_RESULT_SUMMARY_POLLING_INTERVAL = "readResultSummaryPollingInterval";
  private static final String KEY_READ_RESULT_DETAILED_OBFUSCATED_AMOUNT = "readResultDetailedObfuscatedAmount";
  private static final String KEY_READ_RESULT_DETAILED_OBFUSCATED_INTERVAL = "readResultDetailedObfuscatedInterval";
  private static final String KEY_PORTAL_LINK = "passthroughPortalLink";
  private static final String KEY_CCDL_VERSION = "passthroughCcdlVersion";
  private static final String KEY_REST_API_PATH = "uiBackendApiPath";
  private static final String KEY_POLLING_TIME_UI = "passthroughPollingTimeUi";
  private static final String KEY_MAX_SAVED_QUERIES_PER_USER = "maxSavedQueriesPerUser";


  @Value("${passthrough.dsePatientProfileUrl}")
  private String dseUrl;

  @Value("${app.privacy.quota.read.resultDetailedObfuscated.pollingInterval}")
  private String readResultDetailedObfuscatedPollingInterval;

  @Value("${app.privacy.quota.read.resultSummary.pollingInterval}")
  private String readResultSummaryPollingInterval = "PT5S";

  @Value("${passthrough.pollingSummary}")
  private String passthroughPollingSummary = "PT10S";

  @Value("${passthrough.portalLink}")
  private String portalLink;

  @Value("${passthrough.ccdlVersion}")
  private String ccdlVersion = "version";

  @Value("${passthrough.pollingTimeUi}")
  private String pollingTimeUi;

  @Value("${app.privacy.quota.read.resultDetailedObfuscated.amount}")
  private Integer readResultDetailedObfuscatedAmount;

  @Value("${app.privacy.quota.read.resultDetailedObfuscated.interval}")
  private String readResultDetailedObfuscatedInterval;

  @Value("${app.maxSavedQueriesPerUser}")
  private Integer maxSavedQueriesPerUser;


  @GetMapping("/.settings")
  public Map<String, Object> getSettings() {
    Duration backendPollingSummaryLimit;
    Duration uiPollingSummaryLimit;
    try {
      backendPollingSummaryLimit = Duration.parse(readResultSummaryPollingInterval);
    } catch (DateTimeParseException e) {
      backendPollingSummaryLimit = Duration.ofSeconds(5);
    }
    try {
      uiPollingSummaryLimit = Duration.parse(passthroughPollingSummary);
    } catch (DateTimeParseException e) {
      uiPollingSummaryLimit = Duration.ofSeconds(10);
    }
    var exposedPollingSummaryLimit =
        backendPollingSummaryLimit.compareTo(uiPollingSummaryLimit) < 0 ? uiPollingSummaryLimit : backendPollingSummaryLimit.plusSeconds(5);

    return Map.ofEntries(
        Map.entry(KEY_REST_API_PATH, WebSecurityConfig.PATH_API),
        Map.entry(KEY_READ_RESULT_SUMMARY_POLLING_INTERVAL, exposedPollingSummaryLimit.toString()),
        Map.entry(KEY_READ_RESULT_DETAILED_OBFUSCATED_POLLING_INTERVAL, readResultDetailedObfuscatedPollingInterval),
        Map.entry(KEY_READ_RESULT_DETAILED_OBFUSCATED_AMOUNT, readResultDetailedObfuscatedAmount),
        Map.entry(KEY_READ_RESULT_DETAILED_OBFUSCATED_INTERVAL, readResultDetailedObfuscatedInterval),
        Map.entry(KEY_POLLING_TIME_UI, pollingTimeUi),
        Map.entry(KEY_CCDL_VERSION, ccdlVersion),
        Map.entry(KEY_PORTAL_LINK, portalLink),
        Map.entry(KEY_DSE_URL, dseUrl),
        Map.entry(KEY_MAX_SAVED_QUERIES_PER_USER, maxSavedQueriesPerUser)
    );
  }
}
