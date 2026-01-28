package de.medizininformatikinitiative.dataportal.backend.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.medizininformatikinitiative.dataportal.backend.query.api.Ccdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.Crtdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.DataExtraction;
import de.medizininformatikinitiative.dataportal.backend.query.api.Dataquery;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssue;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssueType;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssueValue;
import de.medizininformatikinitiative.dataportal.backend.query.api.validation.JsonSchemaValidator;
import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;

@Service
public class ValidationService {

  private final SmartValidator validator;

  private final JsonSchemaValidator jsonSchemaValidator;

  private ObjectMapper jsonUtil;

  public ValidationService(@NonNull SmartValidator validator,
                           @NonNull JsonSchemaValidator jsonSchemaValidator,
                           @NonNull ObjectMapper jsonUtil) {
    this.validator = validator;
    this.jsonSchemaValidator = jsonSchemaValidator;
    this.jsonUtil = jsonUtil;
  }

  public boolean isValid(Ccdl ccdl) {
    try {
      validateCcdlContent(ccdl);
      return true;
    } catch (MethodArgumentNotValidException | NoSuchMethodException e) {
      return false;
    }
  }

  public boolean isValid(Crtdl crtdl) {
    try {
      validateCrtdlContent(crtdl);
      return true;
    } catch (MethodArgumentNotValidException | NoSuchMethodException e) {
      return false;
    }
  }

  public boolean isValid(Dataquery dataquery) {
    try {
      validateDataqueryContent(dataquery);
      return true;
    } catch (MethodArgumentNotValidException | NoSuchMethodException e) {
      return false;
    }
  }

  public boolean isValid(DataExtraction dataExtraction) {
    try {
      validateDataExtractionContent(dataExtraction);
      return true;
    } catch (MethodArgumentNotValidException | NoSuchMethodException e) {
      return false;
    }
  }

  public void validateDataExtractionContent(DataExtraction dataExtraction) throws NoSuchMethodException, MethodArgumentNotValidException {
    var bindingResult = new BeanPropertyBindingResult(dataExtraction, "dataExtraction");
    validator.validate(dataExtraction, bindingResult);
    if (bindingResult.hasErrors()) {
      var methodParameter = new MethodParameter(this.getClass().getDeclaredMethod("validateDataExtractionContent", DataExtraction.class), 0);
      throw new MethodArgumentNotValidException(methodParameter, bindingResult);
    }
  }

  public void validateCcdlContent(Ccdl ccdl) throws NoSuchMethodException, MethodArgumentNotValidException {
    var bindingResult = new BeanPropertyBindingResult(ccdl, "ccdl");
    validator.validate(ccdl, bindingResult);
    if (bindingResult.hasErrors()) {
      var methodParameter = new MethodParameter(this.getClass().getDeclaredMethod("validateCcdlContent", Ccdl.class), 0);
      throw new MethodArgumentNotValidException(methodParameter, bindingResult);
    }
  }

  public void validateCrtdlContent(Crtdl crtdl) throws NoSuchMethodException, MethodArgumentNotValidException {
    var bindingResult = new BeanPropertyBindingResult(crtdl, "crtdl");
    validator.validate(crtdl, bindingResult);
    if (bindingResult.hasErrors()) {
      var methodParameter = new MethodParameter(this.getClass().getDeclaredMethod("validateCrtdlContent", Crtdl.class), 0);
      throw new MethodArgumentNotValidException(methodParameter, bindingResult);
    }
  }

  public void validateDataqueryContent(Dataquery dataquery) throws NoSuchMethodException, MethodArgumentNotValidException {
    var bindingResult = new BeanPropertyBindingResult(dataquery, "dataquery");
    validator.validate(dataquery, bindingResult);
    if (bindingResult.hasErrors()) {
      var methodParameter = new MethodParameter(this.getClass().getDeclaredMethod("validateDataqueryContent", Dataquery.class), 0);
      throw new MethodArgumentNotValidException(methodParameter, bindingResult);
    }
  }

  public List<ValidationIssue> validateCcdlSchema(JsonNode ccdlNode) {
    return validateSchema(ccdlNode, JsonSchemaValidator.SCHEMA_CCDL);
  }

  public List<ValidationIssue> validateDataquerySchema(JsonNode dataqueryNode) {
    return validateSchema(dataqueryNode, JsonSchemaValidator.SCHEMA_DATAQUERY);
  }

  public List<ValidationIssue> validateCrtdlSchema(JsonNode crtdlNode) {
    return validateSchema(crtdlNode, JsonSchemaValidator.SCHEMA_CRTDL);
  }

  public List<ValidationIssue> validateDataExtractionSchema(JsonNode dataExtractionNode) {
    return validateSchema(dataExtractionNode, JsonSchemaValidator.SCHEMA_DATAEXTRACTION);
  }

  public Ccdl ccdlFromJsonNode(JsonNode jsonNode) {
    return jsonUtil.convertValue(jsonNode, Ccdl.class);
  }

  public Crtdl crtdlFromJsonNode(JsonNode jsonNode) {
    return jsonUtil.convertValue(jsonNode, Crtdl.class);
  }

  public Dataquery dataqueryFromJsonNode(JsonNode jsonNode) {
    return jsonUtil.convertValue(jsonNode, Dataquery.class);
  }

  public DataExtraction dataExtractionFromJsonNode(JsonNode jsonNode) {
    return jsonUtil.convertValue(jsonNode, DataExtraction.class);
  }

  private List<ValidationIssue> validateSchema(JsonNode node, String schemaName) {
    List<ValidationIssue> issues = new ArrayList<>();
    var validationErrors = jsonSchemaValidator.validate(schemaName, node);
    if (!validationErrors.isEmpty()) {
      issues = validationErrors.stream()
          .map(e -> ValidationIssue.builder()
              .path(e.getInstanceLocation().toString())
              .value(ValidationIssueValue.builder()
                  .message(e.getMessage())
                  .code("VALIDATION-" + ValidationIssueType.JSON_ERROR.code())
                  .build())
              .build()
          )
          .toList();
    }
    return issues;
  }
}
