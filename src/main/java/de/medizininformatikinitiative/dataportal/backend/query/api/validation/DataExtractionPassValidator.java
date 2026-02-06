package de.medizininformatikinitiative.dataportal.backend.query.api.validation;

import de.medizininformatikinitiative.dataportal.backend.query.api.DataExtraction;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link DataExtraction} that always passes no matter what instance gets checked.
 */
public class DataExtractionPassValidator implements ConstraintValidator<DataExtractionValidation, DataExtraction> {
  @Override
  public boolean isValid(DataExtraction dataExtraction, ConstraintValidatorContext constraintValidatorContext) {
    return true;
  }
}
