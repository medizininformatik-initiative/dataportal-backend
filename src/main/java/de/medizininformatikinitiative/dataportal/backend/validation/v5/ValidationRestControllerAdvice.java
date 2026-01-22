package de.medizininformatikinitiative.dataportal.backend.validation.v5;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestControllerAdvice("de.medizininformatikinitiative.dataportal.backend.validation.v5")
public class ValidationRestControllerAdvice {

  private final ObjectMapper jsonUtil = new ObjectMapper();

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<List<Map<String, Object>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    List<Map<String, Object>> errors = new ArrayList<>();

    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      String msg = error.getDefaultMessage();
      try {
        Map<String, Object> parsed = jsonUtil.readValue(msg, Map.class);
        parsed.put("path", error.getField().replace(".", "/") + parsed.get("path"));
        errors.add(parsed);
      } catch (Exception e) {
        errors.add(Map.of(
            "path", "/" + error.getField().replace(".", "/"),
            "value", Map.of(
                "code", "VALIDATION_ERROR",
                "message", msg
            )
        ));
      }
    }

    for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
      String msg = error.getDefaultMessage();
      try {
        Map<String,Object> parsed = jsonUtil.readValue(msg, Map.class);
        errors.add(parsed);
      } catch (Exception e) {
        errors.add(Map.of(
            "path", "/",  // fallback path, or customize
            "value", Map.of("code", "VALIDATION_ERROR", "message", msg)
        ));
      }
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
  }
}
