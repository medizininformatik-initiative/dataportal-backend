package de.medizininformatikinitiative.dataportal.backend.query.v5;

import com.fasterxml.jackson.databind.JsonNode;
import de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig;
import de.medizininformatikinitiative.dataportal.backend.query.QueryHandlerService;
import de.medizininformatikinitiative.dataportal.backend.query.QueryHandlerService.ResultDetail;
import de.medizininformatikinitiative.dataportal.backend.query.QueryNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.api.QueryResult;
import de.medizininformatikinitiative.dataportal.backend.query.api.QueryResultRateLimit;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.FeasibilityIssue;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.FeasibilityIssues;
import de.medizininformatikinitiative.dataportal.backend.query.dispatch.QueryDispatchException;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.UserBlacklist;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.UserBlacklistRepository;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.AuthenticationHelper;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.InvalidAuthenticationException;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.RateLimitingService;
import de.medizininformatikinitiative.dataportal.backend.query.translation.QueryTranslationException;
import de.medizininformatikinitiative.dataportal.backend.validation.ContentValidationException;
import de.medizininformatikinitiative.dataportal.backend.validation.ValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.threeten.extra.PeriodDuration;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.*;

/*
Rest Interface for the UI to send queries from the ui to the ui backend.
*/
@RequestMapping(PATH_API + PATH_QUERY + PATH_FEASIBILITY)
@RestController("FeasibilityQueryHandlerRestController-v5")
@Slf4j
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = {HttpHeaders.LOCATION, HttpHeaders.RETRY_AFTER})
public class FeasibilityQueryHandlerRestController {

  public static final String HEADER_X_DETAILED_OBFUSCATED_RESULT_WAS_EMPTY = "X-Detailed-Obfuscated-Result-Was-Empty";
  private final QueryHandlerService queryHandlerService;
  private final ValidationService validationService;
  private final RateLimitingService rateLimitingService;
  private final UserBlacklistRepository userBlacklistRepository;
  private final AuthenticationHelper authenticationHelper;
  private final String apiBaseUrl;

  @Value("${app.keycloakAdminRole}")
  private String keycloakAdminRole;
  @Value("${app.keycloakPowerRole}")
  private String keycloakPowerRole;

  @Value("${app.privacy.quota.soft.create.amount}")
  private int quotaSoftCreateAmount;

  @Value("${app.privacy.quota.soft.create.interval}")
  private String quotaSoftCreateInterval;

  @Value("${app.privacy.quota.hard.create.amount}")
  private int quotaHardCreateAmount;

  @Value("${app.privacy.quota.hard.create.interval}")
  private String quotaHardCreateInterval;

  @Value("${app.privacy.threshold.sites}")
  private int privacyThresholdSites;

  @Value("${app.privacy.threshold.results}")
  private int privacyThresholdResults;

  @Value("${app.privacy.threshold.sitesResult}")
  private int privacyThresholdSitesResult;

  public FeasibilityQueryHandlerRestController(QueryHandlerService queryHandlerService,
                                               RateLimitingService rateLimitingService,
                                               ValidationService validationService,
                                               UserBlacklistRepository userBlacklistRepository,
                                               AuthenticationHelper authenticationHelper,
                                               @Value("${app.apiBaseUrl}") String apiBaseUrl) {
    this.queryHandlerService = queryHandlerService;
    this.rateLimitingService = rateLimitingService;
    this.validationService = validationService;
    this.userBlacklistRepository = userBlacklistRepository;
    this.authenticationHelper = authenticationHelper;
    this.apiBaseUrl = apiBaseUrl;
  }

  @PostMapping
  public ResponseEntity<Object> runQuery(
      @RequestBody JsonNode queryNode,
      @Context HttpServletRequest request,
      Authentication authentication)
      throws InvalidAuthenticationException, NoSuchMethodException {

    var schemaValidationErrors = validationService.validateCcdlSchema(queryNode);
    if (!schemaValidationErrors.isEmpty()) {
      return new ResponseEntity<>(schemaValidationErrors, HttpStatus.BAD_REQUEST);
    }

    var query = validationService.ccdlFromJsonNode(queryNode);
    try {
      validationService.validateCcdlContent(query);
    } catch (MethodArgumentNotValidException e) {
      throw new ContentValidationException(e);
    }

    String userId = authentication.getName();
    Optional<UserBlacklist> userBlacklistEntry = userBlacklistRepository.findByUserId(
        userId);
    boolean isPowerUser = authenticationHelper.hasAuthority(authentication,
        keycloakPowerRole);

    if (!isPowerUser && userBlacklistEntry.isPresent()) {
      var issues = FeasibilityIssues.builder()
              .issues(List.of(FeasibilityIssue.USER_BLACKLISTED_NOT_POWER_USER))
              .build();
      return new ResponseEntity<>(issues,
              HttpStatus.FORBIDDEN);
    }

    Long amountOfQueriesByUserAndHardInterval = queryHandlerService.getAmountOfQueriesByUserAndInterval(
        userId, quotaHardCreateInterval);
    if (!isPowerUser && (quotaHardCreateAmount
        <= amountOfQueriesByUserAndHardInterval)) {
      var intervalEnd = LocalDateTime.now();
      var intervalStart = intervalEnd.minus(PeriodDuration.parse(quotaHardCreateInterval));
      log.info(
          "Blacklisting user {} for exceeding quota without being poweruser. Allowed: {} queries per {}. The user posted {} queries between {} and {}",
          userId,
          quotaHardCreateAmount,
          quotaHardCreateInterval,
          amountOfQueriesByUserAndHardInterval,
          intervalStart,
          intervalEnd);
      UserBlacklist userBlacklist = new UserBlacklist();
      userBlacklist.setUserId(userId);
      userBlacklistRepository.save(userBlacklist);

      var issues = FeasibilityIssues.builder()
              .issues(List.of(FeasibilityIssue.USER_BLACKLISTED_NOT_POWER_USER))
              .build();
      return new ResponseEntity<>(issues,
              HttpStatus.FORBIDDEN);
    }
    Long amountOfQueriesByUserAndSoftInterval = queryHandlerService.getAmountOfQueriesByUserAndInterval(
        userId, quotaSoftCreateInterval);
    if (quotaSoftCreateAmount <= amountOfQueriesByUserAndSoftInterval) {
      Long retryAfter = queryHandlerService.getRetryAfterTime(userId,
          quotaSoftCreateAmount - 1, quotaSoftCreateInterval);
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.add(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
      var issues = FeasibilityIssues.builder()
              .issues(List.of(FeasibilityIssue.QUOTA_EXCEEDED))
              .build();
      return new ResponseEntity<>(issues, httpHeaders,
              HttpStatus.TOO_MANY_REQUESTS);
    }

//    return queryHandlerService.runQuery(query, userId)
//        .map(queryId -> buildResultLocationUri(request, queryId))
//        .map(resultLocation -> ResponseEntity.created(resultLocation).build())
//        .onErrorResume(e -> {
//          log.error("running a query for '%s' failed".formatted(userId), e);
//          return Mono.just(ResponseEntity.internalServerError()
//              .body(e.getMessage()));
//        }).block();

    try {
      var queryId = queryHandlerService.runQueryAsync(query, userId);
      return ResponseEntity
          .created(buildResultLocationUri(queryId))
          .build();
    } catch (QueryDispatchException e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  private URI buildResultLocationUri(Long queryId) {
    return ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(queryId)
        .toUri();
  }

  @GetMapping("/{id}" + WebSecurityConfig.PATH_DETAILED_RESULT)
  public QueryResult getDetailedQueryResult(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId, ResultDetail.DETAILED);
  }

  @GetMapping("/{id}" + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT)
  public ResponseEntity<Object> getDetailedObfuscatedQueryResult(@PathVariable("id") Long queryId,
                                                                 Authentication authentication) {
    if (!hasAccess(queryId, authentication)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    QueryResult queryResult = queryHandlerService.getQueryResult(queryId,
        ResultDetail.DETAILED_OBFUSCATED);

    if (queryResult.totalNumberOfPatients() < privacyThresholdResults) {
      var issues = FeasibilityIssues.builder()
          .issues(List.of(FeasibilityIssue.PRIVACY_RESTRICTION_RESULT_SIZE))
          .build();
      return new ResponseEntity<>(issues,
          HttpStatus.OK);
    }
    HttpHeaders headers = new HttpHeaders();
    if (queryResult.resultLines().stream().filter(result -> result.numberOfPatients() > privacyThresholdSitesResult).count() < privacyThresholdSites) {
      var issues = FeasibilityIssues.builder()
          .issues(List.of(FeasibilityIssue.PRIVACY_RESTRICTION_RESULT_SITES))
          .build();
      return new ResponseEntity<>(
          issues,
          HttpStatus.OK);
    }
    if (queryResult.resultLines().isEmpty()) {
      headers.add(HEADER_X_DETAILED_OBFUSCATED_RESULT_WAS_EMPTY, "true");
    }
    return new ResponseEntity<>(queryResult, headers, HttpStatus.OK);
  }

  @GetMapping("/detailed-obfuscated-result-rate-limit")
  public ResponseEntity<Object> getDetailedObfuscatedResultRateLimit(
      Principal principal) {
    var userId = principal.getName();
    var bucket = this.rateLimitingService.resolveViewDetailedObfuscatedBucket(
        userId);

    QueryResultRateLimit resultRateLimit = QueryResultRateLimit.builder()
        .limit(this.rateLimitingService.getAmountDetailedObfuscated())
        .remaining(bucket.getAvailableTokens())
        .build();

    return new ResponseEntity<>(resultRateLimit, HttpStatus.OK);
  }

  @GetMapping("/quota")
  public ResponseEntity<Object> getQueryCreateQuota(Authentication authentication) {
    var sentQueryStatistics = queryHandlerService.getSentQueryStatistics(authentication.getName(),
        quotaSoftCreateAmount,
        quotaSoftCreateInterval,
        quotaHardCreateAmount,
        quotaHardCreateInterval);
    return new ResponseEntity<>(sentQueryStatistics, HttpStatus.OK);
  }

  @GetMapping("/{id}" + WebSecurityConfig.PATH_SUMMARY_RESULT)
  public ResponseEntity<Object> getSummaryQueryResult(
      @PathVariable("id") Long queryId,
      Authentication authentication) {
    if (!hasAccess(queryId, authentication)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    var queryResult = queryHandlerService.getQueryResult(queryId,
        ResultDetail.SUMMARY);

    if (queryResult.totalNumberOfPatients() < privacyThresholdResults) {
      var issues = FeasibilityIssues.builder()
          .issues(List.of(FeasibilityIssue.PRIVACY_RESTRICTION_RESULT_SIZE))
          .build();
      return new ResponseEntity<>(
          issues,
          HttpStatus.OK);
    }
    return new ResponseEntity<>(queryResult, HttpStatus.OK);
  }

  @PostMapping(value = "/cql")
  public ResponseEntity<?> sq2Cql(@RequestBody JsonNode queryNode) {
    var validationErrors = validationService.validateCcdlSchema(queryNode);
    if (!validationErrors.isEmpty()) {
      return new ResponseEntity<>(validationErrors, HttpStatus.BAD_REQUEST);
    }

    var query = validationService.ccdlFromJsonNode(queryNode);
    try {
      var cql = queryHandlerService.translateQueryToCql(query);
      var sanitizedCql = StringEscapeUtils.escapeHtml4(cql);
      var headers = new HttpHeaders();
      headers.add(HttpHeaders.CONTENT_TYPE, "text/cql;charset=UTF-8");
      return new ResponseEntity<>(sanitizedCql, headers, HttpStatus.OK);
    } catch (QueryTranslationException e) {
      return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }

  private boolean hasAccess(Long queryId, Authentication authentication) {
    Set<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

    try {
      return (roles.contains(keycloakAdminRole)
          || queryHandlerService.getAuthorId(queryId)
          .equalsIgnoreCase(authentication.getName()));
    } catch (QueryNotFoundException e) {
      return false;
    }
  }
}
