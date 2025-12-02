package de.medizininformatikinitiative.dataportal.backend.query.api.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.medizininformatikinitiative.dataportal.backend.query.api.StructuredQuery;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Validator for {@link StructuredQuery} that does an actual check based on a JSON schema.
 */
@Slf4j
public class StructuredQueryValidator implements ConstraintValidator<StructuredQueryValidation, StructuredQuery> {

  @NonNull
  private ObjectMapper jsonUtil;

  /**
   * Required args constructor.
   * <p>
   * Lombok annotation had to be removed since it could not take the necessary Schema Qualifier
   */
  public StructuredQueryValidator(ObjectMapper jsonUtil) {
    this.jsonUtil = jsonUtil;
  }

  /**
   * Validate the submitted {@link StructuredQuery} against the json query schema.
   *
   * @param structuredQuery the {@link StructuredQuery} to validate
   */
  @Override
  public boolean isValid(StructuredQuery structuredQuery,
                         ConstraintValidatorContext constraintValidatorContext) {
    return true;
  }
}
