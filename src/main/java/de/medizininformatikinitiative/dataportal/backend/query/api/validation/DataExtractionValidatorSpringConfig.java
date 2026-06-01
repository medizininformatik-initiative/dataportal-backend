package de.medizininformatikinitiative.dataportal.backend.query.api.validation;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import de.medizininformatikinitiative.dataportal.backend.dse.DseService;
import de.medizininformatikinitiative.dataportal.backend.query.api.DataExtraction;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.CodeableConceptService;
import jakarta.validation.ConstraintValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class DataExtractionValidatorSpringConfig {

  @Value("${app.enableQueryValidation}")
  private boolean enabled;

  @Bean
  public ConstraintValidator<DataExtractionValidation, DataExtraction> createDataExtractionValidator(
      CodeableConceptService codeableConceptService,
      DseService dseService) {
    return enabled
        ? new DataExtractionValidator(codeableConceptService, dseService, JsonMapper.builderWithJackson2Defaults().build())
        : new DataExtractionPassValidator();
  }
}
