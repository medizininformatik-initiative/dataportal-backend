package de.numcodex.feasibility_gui_backend.query.api.validation;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.status.ValidationIssue;
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
   * @param validationIssue    Error Enum
   */
  public static void addError(
      ConstraintValidatorContext context,
      String jsonPointer,
      ValidationIssue validationIssue
  ) {
    addError(context, jsonPointer, "VALIDATION-" + validationIssue.code(), validationIssue.detail(), null);
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
    addError(context, jsonPointer, code, message, null);
  }

  /**
   * Adds a structured error including optional extra details.
   *
   * @param context ConstraintValidatorContext from validator
   * @param jsonPointer    jsonPointer (RFC 6901)
   * @param validationIssue    Error Enum
   * @param extra   Optional extra fields (e.g., allowedValues, rejectedValue)
   */
  public static void addError(
      ConstraintValidatorContext context,
      String jsonPointer,
      ValidationIssue validationIssue,
      Map<String, Object> extra
  ) {
    addError(context, jsonPointer, "VALIDATION-" + validationIssue.code(), validationIssue.detail(), extra);
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
      Map<String, Object> mergedValue = extra != null
          ? new java.util.LinkedHashMap<>(value) {{ putAll(extra); }}
          : value;

      Map<String, Object> error = Map.of(
          "path", jsonPointer,
          "value", mergedValue
      );
      String jsonMessage = jsonUtil.writeValueAsString(error);
      context.buildConstraintViolationWithTemplate(jsonMessage).addConstraintViolation();
    } catch (Exception e) {
      throw new RuntimeException("Failed to build validation error", e);
    }
  }
}
