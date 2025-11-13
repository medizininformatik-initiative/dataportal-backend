package de.numcodex.feasibility_gui_backend.query.api.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import de.numcodex.feasibility_gui_backend.terminology.es.CodeableConceptService;
import de.numcodex.feasibility_gui_backend.terminology.es.TerminologyEsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.ConstraintValidator;

@Configuration
@Slf4j
public class StructuredQueryValidatorSpringConfig {

  @Value("${app.enableQueryValidation}")
  private boolean enabled;

  @Bean
  public ConstraintValidator<StructuredQueryValidation, StructuredQuery> createQueryValidator(
      TerminologyService terminologyService,
      TerminologyEsService terminologyEsService,
      CodeableConceptService codeableConceptService) {
    return enabled
        ? new StructuredQueryValidator(terminologyService, terminologyEsService, codeableConceptService, new ObjectMapper())
        : new StructuredQueryPassValidator();
  }
}
