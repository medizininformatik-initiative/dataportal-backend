package de.numcodex.feasibility_gui_backend.query.api.validation;

import tools.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.ConstraintValidator;

@Configuration
@Slf4j
public class StructuredQueryValidatorSpringConfig {

  private static final String JSON_SCHEMA = "/de/numcodex/feasibility_gui_backend/query/api/validation/query-schema.json";

  @Value("${app.enableQueryValidation}")
  private boolean enabled;

  @Bean
  public ConstraintValidator<StructuredQueryValidation, StructuredQuery> createQueryValidator() {
    return enabled
            ? new StructuredQueryValidator(new ObjectMapper())
            : new StructuredQueryPassValidator();
  }
}
