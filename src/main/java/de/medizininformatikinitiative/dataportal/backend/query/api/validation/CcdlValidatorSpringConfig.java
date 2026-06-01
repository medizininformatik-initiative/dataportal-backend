package de.medizininformatikinitiative.dataportal.backend.query.api.validation;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import de.medizininformatikinitiative.dataportal.backend.query.api.Ccdl;
import de.medizininformatikinitiative.dataportal.backend.terminology.TerminologyService;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.CodeableConceptService;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.TerminologyEsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.ConstraintValidator;

@Configuration
@Slf4j
public class CcdlValidatorSpringConfig {

  @Value("${app.enableQueryValidation}")
  private boolean enabled;

  @Bean
  public ConstraintValidator<CcdlValidation, Ccdl> createQueryValidator(
      TerminologyService terminologyService,
      TerminologyEsService terminologyEsService,
      CodeableConceptService codeableConceptService) {
    return enabled
        ? new CcdlValidator(terminologyService, terminologyEsService, codeableConceptService, JsonMapper.builderWithJackson2Defaults().build())
        : new CcdlPassValidator();
  }
}
