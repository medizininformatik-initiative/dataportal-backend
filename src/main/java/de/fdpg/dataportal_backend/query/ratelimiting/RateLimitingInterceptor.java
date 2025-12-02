package de.fdpg.dataportal_backend.query.ratelimiting;

import de.fdpg.dataportal_backend.config.WebSecurityConfig;
import de.fdpg.dataportal_backend.query.api.status.FeasibilityIssue;
import de.fdpg.dataportal_backend.query.v5.FeasibilityQueryHandlerRestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * This Interceptor checks whether a user may use the requested endpoint at this
 * moment. If the user has the admin role (as defined via config), he is not
 * subject to rate-limiting.
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

  private static final String HEADER_LIMIT_REMAINING = "X-Rate-Limit-Remaining";
  private static final String HEADER_RETRY_AFTER = "X-Rate-Limit-Retry-After-Seconds";

  private static final String HEADER_LIMIT_REMAINING_DETAILED_OBFUSCATED_RESULTS = "X-Rate-Limit-Detailed-Obfuscated-Results-Remaining";
  private static final String HEADER_RETRY_AFTER_DETAILED_OBFUSCATED_RESULTS = "X-Rate-Limit-Detailed-Obfuscated-Results-Retry-After-Seconds";
  private final RateLimitingService rateLimitingService;

  private final AuthenticationHelper authenticationHelper;

  @Value("${app.keycloakAllowedRole}")
  private String keycloakAllowedRole;

  @Value("${app.keycloakAdminRole}")
  private String keycloakAdminRole;

  @Autowired
  public RateLimitingInterceptor(RateLimitingService rateLimitingService,
      AuthenticationHelper authenticationHelper) {
    this.rateLimitingService = rateLimitingService;
    this.authenticationHelper = authenticationHelper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request,
      HttpServletResponse response, Object handler)
      throws Exception {
    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      response.sendError(HttpStatus.UNAUTHORIZED.value());
      return false;
    }

    if (authenticationHelper.hasAuthority(authentication, keycloakAdminRole)) {
      return true;
    } else if (!authenticationHelper.hasAuthority(authentication,
        keycloakAllowedRole)) {
      response.sendError(HttpStatus.FORBIDDEN.value());
      return false;
    }

    // Handle Summary Result
    if ((request.getRequestURI()
        .endsWith(WebSecurityConfig.PATH_SUMMARY_RESULT))) {
      var summaryResultTokenBucket = rateLimitingService.resolveSummaryResultBucket(
          authentication.getName());
      var summaryResultProbe = summaryResultTokenBucket.tryConsumeAndReturnRemaining(
          1);
      if (summaryResultProbe.isConsumed()) {
        response.addHeader(HEADER_LIMIT_REMAINING,
            Long.toString(summaryResultProbe.getRemainingTokens()));
        return true;
      } else {
        long waitForRefill =
            summaryResultProbe.getNanosToWaitForRefill() / 1_000_000_000;
        response.addHeader(HEADER_RETRY_AFTER, Long.toString(waitForRefill));
        response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                Integer.toString(FeasibilityIssue.POLLING_LIMIT_EXCEEDED.code()));
        return false;
      }

    }

    // Handle Detailed Obfuscated Result
    if ((request.getRequestURI()
        .endsWith(WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT))) {
      var detailedPollingResultTokenBucket = rateLimitingService.resolveDetailedObfuscatedResultBucket(
          authentication.getName());
      var detailedPollingResultProbe = detailedPollingResultTokenBucket.tryConsumeAndReturnRemaining(
          1);
      if (detailedPollingResultProbe.isConsumed()) {

        response.addHeader(HEADER_LIMIT_REMAINING,
            Long.toString(detailedPollingResultProbe.getRemainingTokens()));

        var detailedObfuscatedResultTokenBucket = rateLimitingService.resolveViewDetailedObfuscatedBucket(
            authentication.getName());
        var detailedObfuscatedResultProbe = detailedObfuscatedResultTokenBucket.tryConsumeAndReturnRemaining(
            1);

        if (detailedObfuscatedResultProbe.isConsumed()) {
          response.addHeader(HEADER_LIMIT_REMAINING_DETAILED_OBFUSCATED_RESULTS,
              Long.toString(
                  detailedObfuscatedResultProbe.getRemainingTokens()));
          return true;
        } else {
          long waitForRefill =
              detailedObfuscatedResultProbe.getNanosToWaitForRefill()
                  / 1_000_000_000;
          response.addHeader(HEADER_RETRY_AFTER_DETAILED_OBFUSCATED_RESULTS,
              Long.toString(waitForRefill));
          response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), Integer.toString(FeasibilityIssue.QUOTA_EXCEEDED.code()));
          return false;
        }
      } else {
        long waitForRefill =
            detailedPollingResultProbe.getNanosToWaitForRefill()
                / 1_000_000_000;
        response.addHeader(HEADER_RETRY_AFTER, Long.toString(waitForRefill));
        response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), Integer.toString(FeasibilityIssue.POLLING_LIMIT_EXCEEDED.code()));
        return false;
      }
    }

    return false;

  }

  @Override
  public void afterCompletion(HttpServletRequest request,
      HttpServletResponse response,
      Object handler, Exception ex) throws Exception {
    HandlerInterceptor.super.afterCompletion(request, response, handler, ex);

    if (request.getRequestURI()
        .endsWith(WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT) && response.containsHeader(
        FeasibilityQueryHandlerRestController.HEADER_X_DETAILED_OBFUSCATED_RESULT_WAS_EMPTY)) {
      var authentication = SecurityContextHolder.getContext()
          .getAuthentication();
      var detailedObfuscatedResultTokenBucket = rateLimitingService.resolveViewDetailedObfuscatedBucket(
          authentication.getName());
      detailedObfuscatedResultTokenBucket.addTokens(1);
      response.setHeader(
          FeasibilityQueryHandlerRestController.HEADER_X_DETAILED_OBFUSCATED_RESULT_WAS_EMPTY,
          null);
    }
  }
}
