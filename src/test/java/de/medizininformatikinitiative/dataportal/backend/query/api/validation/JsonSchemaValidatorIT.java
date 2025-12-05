package de.medizininformatikinitiative.dataportal.backend.query.api.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonSchemaValidatorIT {
  private JsonSchemaValidator validator;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    validator = new JsonSchemaValidator();
    mapper = new ObjectMapper();
  }

  @Test
  void validateCrtdlSchema_shouldPassForValidJson() throws Exception {
    JsonNode jsonNode = loadJson("crtdl-valid.json");

    List<Error> errors = validator.validate(JsonSchemaValidator.SCHEMA_CRTDL, jsonNode);

    assertThat(errors).isEmpty();
  }

  @Test
  void validateCrtdlSchema_shouldFailForInvalidJson() throws Exception {
    JsonNode jsonNode = loadJson("crtdl-invalid.json");

    List<Error> errors = validator.validate(JsonSchemaValidator.SCHEMA_CRTDL, jsonNode);

    assertThat(errors).isNotEmpty();
  }

  @Test
  void validateCcdlSchema_shouldPassForValidJson() throws Exception {
    JsonNode jsonNode = loadJson("ccdl-valid.json");

    List<Error> errors = validator.validate(JsonSchemaValidator.SCHEMA_CCDL, jsonNode);

    assertThat(errors).isEmpty();
  }

  @Test
  void validateCcdlSchema_shouldFailForInvalidJson() throws Exception {
    JsonNode jsonNode = loadJson("ccdl-invalid.json");

    List<Error> errors = validator.validate(JsonSchemaValidator.SCHEMA_CRTDL, jsonNode);

    assertThat(errors).isNotEmpty();
  }

  @Test
  void validateDataExtractionSchema_shouldPassForValidJson() throws Exception {
    JsonNode jsonNode = loadJson("dataExtraction-valid.json");

    List<Error> errors = validator.validate(JsonSchemaValidator.SCHEMA_DATAEXTRACTION, jsonNode);

    assertThat(errors).isEmpty();
  }

  @Test
  void validateDataExtractionSchema_shouldFailForInvalidJson() throws Exception {
    JsonNode jsonNode = loadJson("dataExtraction-invalid.json");

    List<Error> errors = validator.validate(JsonSchemaValidator.SCHEMA_DATAEXTRACTION, jsonNode);

    assertThat(errors).isNotEmpty();
  }

  @Test
  void validate_shouldThrowExceptionForUnknownSchema() throws Exception {
    JsonNode jsonNode = loadJson("crtdl-valid.json");

    assertThrows(IllegalArgumentException.class, () ->
        validator.validate("unknownSchemaKey", jsonNode));
  }

  private JsonNode loadJson(String resourcePath) throws Exception {
    try (InputStream is = JsonSchemaValidatorIT.class.getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("Resource not found: " + resourcePath);
      }
      return mapper.readTree(is);
    }
  }
}
