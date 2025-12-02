package de.fdpg.dataportal_backend.query.ratelimiting;

import de.fdpg.dataportal_backend.query.api.status.FeasibilityIssue;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;

@Component
public class RateLimitingErrorAttributes extends DefaultErrorAttributes {

  @Override
  public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
    String originalErrorMessage = super.getMessage(webRequest, null);

    try {
      return Map.of("issues", List.of(FeasibilityIssue.valueOf(Integer.parseInt(originalErrorMessage))));
    } catch (IllegalArgumentException e) {
      return super.getErrorAttributes(webRequest, options);
    }
  }
}
