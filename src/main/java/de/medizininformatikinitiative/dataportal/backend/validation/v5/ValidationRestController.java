package de.medizininformatikinitiative.dataportal.backend.validation.v5;

import com.fasterxml.jackson.databind.JsonNode;
import de.medizininformatikinitiative.dataportal.backend.query.QueryHandlerService;
import de.medizininformatikinitiative.dataportal.backend.query.dataquery.DataqueryHandler;
import de.medizininformatikinitiative.dataportal.backend.terminology.validation.CcdlValidation;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.*;

@RequestMapping(PATH_API + PATH_VALIDATION)
@RestController
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = "Location")
public class ValidationRestController {

  private final DataqueryHandler dataqueryHandler;
  private final QueryHandlerService queryHandlerService;
  private final CcdlValidation ccdlValidation;
  private final SmartValidator validator;

  public ValidationRestController(DataqueryHandler dataqueryHandler,
                                  QueryHandlerService queryHandlerService,
                                  CcdlValidation ccdlValidation,
                                  SmartValidator validator) {
    this.dataqueryHandler = dataqueryHandler;
    this.queryHandlerService = queryHandlerService;
    this.ccdlValidation = ccdlValidation;
    this.validator = validator;
  }

  @PostMapping(path = PATH_CRTDL)
  public ResponseEntity<Object> validateCrtdl(@RequestBody JsonNode crtdlNode) {
    var validationErrors = dataqueryHandler.validateCrtdl(crtdlNode);
    if (!validationErrors.isEmpty()) {
      return new ResponseEntity<>(validationErrors, HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PostMapping(PATH_CCDL)
  public ResponseEntity<?> validateCcdl(
      @RequestBody JsonNode queryNode) {
    var validationErrors = queryHandlerService.validateCcdl(queryNode);
    if (!validationErrors.isEmpty()) {
      return new ResponseEntity<>(validationErrors, HttpStatus.BAD_REQUEST);
    }

    var query = queryHandlerService.ccdlFromJsonNode(queryNode);
    return new ResponseEntity<>(ccdlValidation.annotateCcdl(query, false), HttpStatus.OK);
  }

  @PostMapping("/dataquery")
  public ResponseEntity<Object> validateDataquery(
      @RequestBody JsonNode dataqueryJsonNode) throws MethodArgumentNotValidException, NoSuchMethodException {

    // Validate Schema
    var schemaValidationErrors = dataqueryHandler.validateDataquery(dataqueryJsonNode);
    if (!schemaValidationErrors.isEmpty()) {
      return new ResponseEntity<>(schemaValidationErrors, HttpStatus.BAD_REQUEST);
    }

    // Validate Content
    var dataquery = dataqueryHandler.dataqueryFromJsonNode(dataqueryJsonNode);
    var bindingResult = new BeanPropertyBindingResult(dataquery, "dataquery");
    validator.validate(dataquery, bindingResult);
    if (bindingResult.hasErrors()) {
      var methodParameter = new MethodParameter(this.getClass().getDeclaredMethod("validateCcdl", JsonNode.class), 0);
      throw new MethodArgumentNotValidException(methodParameter, bindingResult);
    }

    return new ResponseEntity<>(HttpStatus.OK);
  }
}
