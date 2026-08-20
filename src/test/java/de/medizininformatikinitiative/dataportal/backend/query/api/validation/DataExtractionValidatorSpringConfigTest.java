package de.medizininformatikinitiative.dataportal.backend.query.api.validation;

import de.medizininformatikinitiative.dataportal.backend.dse.DseService;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.CodeableConceptService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Tag("query")
@Tag("api")
@Tag("validation")
@ExtendWith(MockitoExtension.class)
class DataExtractionValidatorSpringConfigTest {

  @Mock
  private CodeableConceptService codeableConceptService;

  @Mock
  private DseService dseService;

  @Test
  void createDataExtractionValidator_returnsRealValidatorWhenEnabled() {
    var config = new DataExtractionValidatorSpringConfig();
    ReflectionTestUtils.setField(config, "enabled", true);

    var validator = config.createDataExtractionValidator(codeableConceptService, dseService);

    assertInstanceOf(DataExtractionValidator.class, validator);
  }

  @Test
  void createDataExtractionValidator_returnsPassValidatorWhenDisabled() {
    var config = new DataExtractionValidatorSpringConfig();
    ReflectionTestUtils.setField(config, "enabled", false);

    var validator = config.createDataExtractionValidator(codeableConceptService, dseService);

    assertInstanceOf(DataExtractionPassValidator.class, validator);
  }
}
