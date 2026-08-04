package de.medizininformatikinitiative.dataportal.backend.validation;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationRestControllerAdviceTest {

  private final ValidationRestControllerAdvice advice = new ValidationRestControllerAdvice();

  @Test
  void handleValidationExceptions_fieldErrorWithJsonMessage_prefixesFieldOntoPath() throws NoSuchMethodException {
    var ex = exceptionWithFieldError("ccdl", "display",
        "{\"path\":\"/display\",\"value\":{\"code\":\"VALIDATION-1\",\"message\":\"Invalid value\"}}");

    var response = advice.handleValidationExceptions(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).hasSize(1);
    var error = response.getBody().get(0);
    assertThat(error.get("path")).isEqualTo("display/display");
    assertThat(value(error).get("code")).isEqualTo("VALIDATION-1");
    assertThat(value(error).get("message")).isEqualTo("Invalid value");
  }

  @Test
  void handleValidationExceptions_fieldErrorWithNonJsonMessage_fallsBackToVerbatimMessage() throws NoSuchMethodException {
    var ex = exceptionWithFieldError("ccdl", "display", "must not be blank");

    var response = advice.handleValidationExceptions(ex);

    var error = response.getBody().get(0);
    assertThat(error.get("path")).isEqualTo("/display");
    assertThat(value(error).get("code")).isEqualTo("VALIDATION_ERROR");
    assertThat(value(error).get("message")).isEqualTo("must not be blank");
  }

  @Test
  void handleValidationExceptions_fieldErrorWithNullMessage_currentlyThrowsNpe() throws NoSuchMethodException {
    // Documents a latent bug: Map.of(...) rejects the null "message" value produced by a
    // FieldError without a default message, so the intended fallback path crashes instead
    // of returning a graceful validation error.
    var bindingResult = new BeanPropertyBindingResult(new Object(), "ccdl");
    bindingResult.addError(new FieldError("ccdl", "display", null, false, null, null, null));
    var ex = new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);

    assertThrows(NullPointerException.class, () -> advice.handleValidationExceptions(ex));
  }

  @Test
  void handleValidationExceptions_globalErrorWithJsonMessage_usesParsedPathVerbatim() throws NoSuchMethodException {
    var bindingResult = new BeanPropertyBindingResult(new Object(), "ccdl");
    bindingResult.addError(new ObjectError("ccdl",
        "{\"path\":\"/inclusionCriteria\",\"value\":{\"code\":\"VALIDATION-2\",\"message\":\"invalid combination\"}}"));
    var ex = new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);

    var response = advice.handleValidationExceptions(ex);

    var error = response.getBody().get(0);
    assertThat(error.get("path")).isEqualTo("/inclusionCriteria");
    assertThat(value(error).get("code")).isEqualTo("VALIDATION-2");
  }

  @Test
  void handleValidationExceptions_globalErrorWithNonJsonMessage_fallsBackToRootPath() throws NoSuchMethodException {
    var bindingResult = new BeanPropertyBindingResult(new Object(), "ccdl");
    bindingResult.addError(new ObjectError("ccdl", "inconsistent object"));
    var ex = new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);

    var response = advice.handleValidationExceptions(ex);

    var error = response.getBody().get(0);
    assertThat(error.get("path")).isEqualTo("/");
    assertThat(value(error).get("code")).isEqualTo("VALIDATION_ERROR");
    assertThat(value(error).get("message")).isEqualTo("inconsistent object");
  }

  @Test
  void handleValidationExceptions_combinesFieldAndGlobalErrorsInOrder() throws NoSuchMethodException {
    var bindingResult = new BeanPropertyBindingResult(new Object(), "ccdl");
    bindingResult.addError(new FieldError("ccdl", "display", "field problem"));
    bindingResult.addError(new ObjectError("ccdl", "global problem"));
    var ex = new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);

    var response = advice.handleValidationExceptions(ex);

    assertThat(response.getBody()).hasSize(2);
    assertThat(value(response.getBody().get(0)).get("message")).isEqualTo("field problem");
    assertThat(value(response.getBody().get(1)).get("message")).isEqualTo("global problem");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> value(Map<String, Object> error) {
    return (Map<String, Object>) error.get("value");
  }

  private MethodArgumentNotValidException exceptionWithFieldError(String objectName, String field, String message) throws NoSuchMethodException {
    var bindingResult = new BeanPropertyBindingResult(new Object(), objectName);
    bindingResult.addError(new FieldError(objectName, field, message));
    return new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);
  }

  private MethodParameter dummyMethodParameter() throws NoSuchMethodException {
    return new MethodParameter(Object.class.getMethod("equals", Object.class), 0);
  }
}
