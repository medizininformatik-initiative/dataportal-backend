package de.medizininformatikinitiative.dataportal.backend.validation.v5;

import com.fasterxml.jackson.databind.JsonNode;
import de.medizininformatikinitiative.dataportal.backend.validation.ContentValidationException;
import de.medizininformatikinitiative.dataportal.backend.validation.ValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.*;

@RequestMapping(PATH_API + PATH_VALIDATION)
@RestController
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = "Location")
public class ValidationRestController {

  private final ValidationService validationService;

  public ValidationRestController(ValidationService validationService) {
    this.validationService = validationService;
  }

  @PostMapping(PATH_CCDL)
  public ResponseEntity<?> validateCcdl(@RequestBody JsonNode queryNode) {
    // Validate Schema
    var schemaValidationErrors = validationService.validateCcdlSchema(queryNode);
    if (!schemaValidationErrors.isEmpty()) {
      return new ResponseEntity<>(schemaValidationErrors, HttpStatus.BAD_REQUEST);
    }

    // Validate Content
    var ccdl = validationService.ccdlFromJsonNode(queryNode);
    try {
      validationService.validateCcdlContent(ccdl);
    } catch (NoSuchMethodException | MethodArgumentNotValidException e) {
      throw new ContentValidationException(e);
    }

    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PostMapping(path = PATH_CRTDL)
  public ResponseEntity<Object> validateCrtdl(@RequestBody JsonNode crtdlNode) {

    // Validate Schema
    var schemaValidationErrors = validationService.validateCrtdlSchema(crtdlNode);
    if (!schemaValidationErrors.isEmpty()) {
      return new ResponseEntity<>(schemaValidationErrors, HttpStatus.BAD_REQUEST);
    }

    // Validate Content
    var crtdl = validationService.crtdlFromJsonNode(crtdlNode);
    try {
      validationService.validateCrtdlContent(crtdl);
    } catch (NoSuchMethodException | MethodArgumentNotValidException e) {
      throw new ContentValidationException(e);
    }

    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PostMapping("/dataquery")
  public ResponseEntity<Object> validateDataquery(
      @RequestBody JsonNode dataqueryJsonNode) {

    // Validate Schema
    var schemaValidationErrors = validationService.validateDataquerySchema(dataqueryJsonNode);
    if (!schemaValidationErrors.isEmpty()) {
      return new ResponseEntity<>(schemaValidationErrors, HttpStatus.BAD_REQUEST);
    }

    // Validate Content
    var dataquery = validationService.dataqueryFromJsonNode(dataqueryJsonNode);
    try {
      validationService.validateDataqueryContent(dataquery);
    } catch (NoSuchMethodException | MethodArgumentNotValidException e) {
      throw new ContentValidationException(e);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
