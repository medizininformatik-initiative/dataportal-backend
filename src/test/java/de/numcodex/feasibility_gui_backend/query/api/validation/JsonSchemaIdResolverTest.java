package de.numcodex.feasibility_gui_backend.query.api.validation;

import com.networknt.schema.AbsoluteIri;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaIdResolverTest {
  private JsonSchemaIdResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new JsonSchemaIdResolver();
  }

  @Test
  void shouldResolveCrtdlSchemaIri() {
    AbsoluteIri input = AbsoluteIri.of(JsonSchemaIdResolver.SCHEMA_IRI_CRTDL_SCHEMA);

    AbsoluteIri result = resolver.resolve(input);

    assertNotNull(result);
    assertEquals(JsonSchemaIdResolver.CLASSPATH_CRTDL_SCHEMA_FILE, result.toString());
  }

  @Test
  void shouldResolveCcdlSchemaIri() {
    AbsoluteIri input = AbsoluteIri.of(JsonSchemaIdResolver.SCHEMA_IRI_CCDL_SCHEMA);

    AbsoluteIri result = resolver.resolve(input);

    assertNotNull(result);
    assertEquals(JsonSchemaIdResolver.CLASSPATH_CCDL_SCHEMA_FILE, result.toString());
  }

  @Test
  void shouldReturnNullForUnknownIri() {
    AbsoluteIri input = AbsoluteIri.of("http://example.com/unknown-schema.json");

    AbsoluteIri result = resolver.resolve(input);

    assertNull(result);
  }

  @Test
  void shouldHandleNullInputGracefully() {
    assertThrows(NullPointerException.class, () -> resolver.resolve(null));
  }
}