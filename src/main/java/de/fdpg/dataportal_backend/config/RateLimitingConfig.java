package de.fdpg.dataportal_backend.config;

import de.fdpg.dataportal_backend.query.ratelimiting.RateLimitingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static de.fdpg.dataportal_backend.config.WebSecurityConfig.*;

@Component
public class RateLimitingConfig implements WebMvcConfigurer {

  @Autowired
  @Lazy
  private RateLimitingInterceptor interceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(interceptor)
        .addPathPatterns(PATH_API + PATH_QUERY + PATH_FEASIBILITY + PATH_ID_MATCHER + PATH_SUMMARY_RESULT)
        .addPathPatterns(PATH_API + PATH_QUERY + PATH_FEASIBILITY + PATH_ID_MATCHER + PATH_DETAILED_OBFUSCATED_RESULT);
  }
}
