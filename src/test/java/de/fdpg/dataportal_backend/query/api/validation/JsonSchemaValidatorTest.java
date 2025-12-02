package de.fdpg.dataportal_backend.query.api.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaValidatorTest {

  private JsonSchemaValidator validator;

  @Mock
  private Schema mockSchema;

  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setup() {
    validator = new JsonSchemaValidator(Map.of(
        JsonSchemaValidator.SCHEMA_CRTDL, mockSchema,
        JsonSchemaValidator.SCHEMA_CCDL, mockSchema,
        JsonSchemaValidator.SCHEMA_DATAEXTRACTION, mockSchema
    ));
  }

  @Test
  void validate_shouldReturnErrors_whenSchemaValidationFails() throws Exception {
    JsonNode jsonNode = mapper.readTree("{\"foo\":\"bar\"}");
    var mockError = mock(Error.class);

    when(mockSchema.validate(any(JsonNode.class), any(Consumer.class))).thenReturn(List.of(mockError));

    List<Error> errors = validator.validate(JsonSchemaValidator.SCHEMA_CRTDL, jsonNode);

    assertThat(errors).containsExactly(mockError);
  }

  @Test
  void validate_shouldReturnEmptyList_whenSchemaValidationSucceeds() throws Exception {
    JsonNode jsonNode = mapper.readTree("{\"foo\":\"bar\"}");
    when(mockSchema.validate(any(JsonNode.class), any(Consumer.class))).thenReturn(List.of());

    List<Error> errors = validator.validate(JsonSchemaValidator.SCHEMA_CCDL, jsonNode);

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_shouldThrowException_whenUnknownSchemaKey() throws Exception {
    JsonNode jsonNode = mapper.readTree("{\"foo\":\"bar\"}");

    assertThatThrownBy(() -> validator.validate("unknownKey", jsonNode))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown schema: unknownKey");
  }

  @Test
  void constructor_shouldLoadSchemasIntoCache() throws NoSuchFieldException {
    JsonSchemaValidator validator = new JsonSchemaValidator();

    Map<String, ?> cache = validator.getClass()
        .getDeclaredField("schemaCache")
        .trySetAccessible()
        ? (Map<String, ?>) getPrivateField(validator, "schemaCache")
        : Map.of();

    assertThat(cache).containsKeys(
        JsonSchemaValidator.SCHEMA_CRTDL,
        JsonSchemaValidator.SCHEMA_CCDL,
        JsonSchemaValidator.SCHEMA_DATAEXTRACTION
    );
  }

  @SuppressWarnings("unchecked")
  private static Object getPrivateField(Object target, String fieldName) {
    try {
      var field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(target);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
