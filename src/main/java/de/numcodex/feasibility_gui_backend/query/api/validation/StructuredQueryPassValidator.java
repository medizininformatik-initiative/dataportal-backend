package de.numcodex.feasibility_gui_backend.query.api.validation;

import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link StructuredQuery} that always passes no matter what instance gets checked.
 */
public class StructuredQueryPassValidator implements ConstraintValidator<StructuredQueryValidation, StructuredQuery> {
  @Override
  public boolean isValid(StructuredQuery structuredQuery, ConstraintValidatorContext constraintValidatorContext) {
    return true;
  }
}
