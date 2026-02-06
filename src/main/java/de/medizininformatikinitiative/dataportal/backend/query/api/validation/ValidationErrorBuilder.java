package de.medizininformatikinitiative.dataportal.backend.query.api.validation;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssueType;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Map;

public class ValidationErrorBuilder {

  private static final ObjectMapper jsonUtil = new ObjectMapper();

  private ValidationErrorBuilder() {}

  /**
   * Adds a structured error to the ConstraintValidatorContext.
   *
   * @param context ConstraintValidatorContext from validator
   * @param jsonPointer    jsonPointer (RFC 6901)
   * @param validationIssueType    Error Enum
   */
  public static void addError(
      ConstraintValidatorContext context,
      String jsonPointer,
      ValidationIssueType validationIssueType
  ) {
    addError(context,
        jsonPointer,
        "VALIDATION-" + validationIssueType.code(),
        validationIssueType.detail(),
        null);
  }

  /**
   * Adds a structured error to the ConstraintValidatorContext.
   *
   * @param context ConstraintValidatorContext from validator
   * @param jsonPointer    jsonPointer (RFC 6901)
   * @param code    Error code (e.g., INVALID_VALUE)
   * @param message Human-readable message
   */
  public static void addError(
      ConstraintValidatorContext context,
      String jsonPointer,
      String code,
      String message
  ) {
    addError(context,
        jsonPointer,
        code,
        message,
        null);
  }

  /**
   * Adds a structured error including optional extra details.
   *
   * @param context ConstraintValidatorContext from validator
   * @param jsonPointer    jsonPointer (RFC 6901)
   * @param validationIssueType    Error Enum
   * @param extra   Optional extra fields (e.g., allowedValues, rejectedValue)
   */
  public static void addError(
      ConstraintValidatorContext context,
      String jsonPointer,
      ValidationIssueType validationIssueType,
      Map<String, Object> extra
  ) {
    addError(context,
        jsonPointer,
        "VALIDATION-" + validationIssueType.code(),
        validationIssueType.detail(),
        extra);
  }

  /**
   * Adds a structured error including optional extra details.
   *
   * @param context ConstraintValidatorContext from validator
   * @param jsonPointer    jsonPointer (RFC 6901)
   * @param code    Error code
   * @param message Human-readable message
   * @param extra   Optional extra fields (e.g., allowedValues, rejectedValue)
   */
  public static void addError(
      ConstraintValidatorContext context,
      String jsonPointer,
      String code,
      String message,
      Map<String, Object> extra
  ) {
    try {
      Map<String, Object> value = Map.of("code", code, "message", message);
      Map<String, Object> error = Map.of(
          "path", jsonPointer,
          "value", value,
          "details", extra == null ? Map.of() : extra
      );
      String jsonMessage = jsonUtil.writeValueAsString(error);
      context.buildConstraintViolationWithTemplate(jsonMessage).addConstraintViolation();
    } catch (Exception e) {
      throw new RuntimeException("Failed to build validation error", e);
    }
  }
}
