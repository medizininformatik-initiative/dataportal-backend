package de.fdpg.dataportal_backend.query.api.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fdpg.dataportal_backend.query.api.StructuredQuery;
import jakarta.validation.ConstraintValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class StructuredQueryValidatorSpringConfig {

  private static final String JSON_SCHEMA = "/de/fdpg/dataportal_backend/query/api/validation/query-schema.json";

  @Value("${app.enableQueryValidation}")
  private boolean enabled;

  @Bean
  public ConstraintValidator<StructuredQueryValidation, StructuredQuery> createQueryValidator() {
    return enabled
        ? new StructuredQueryValidator(new ObjectMapper())
        : new StructuredQueryPassValidator();
  }
}
