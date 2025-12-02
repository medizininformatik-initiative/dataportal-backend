package de.numcodex.feasibility_gui_backend.query.api.validation;

import de.numcodex.feasibility_gui_backend.query.api.Ccdl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link Ccdl} that always passes no matter what instance gets checked.
 */
public class CcdlPassValidator implements ConstraintValidator<CcdlValidation, Ccdl> {
  @Override
  public boolean isValid(Ccdl ccdl, ConstraintValidatorContext constraintValidatorContext) {
    return true;
  }
}
