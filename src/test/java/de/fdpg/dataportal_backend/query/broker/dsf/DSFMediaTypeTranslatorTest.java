package de.fdpg.dataportal_backend.query.broker.dsf;

import de.fdpg.dataportal_backend.query.QueryMediaType;
import de.fdpg.dataportal_backend.query.broker.UnsupportedMediaTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DSFMediaTypeTranslatorTest {

  private DSFMediaTypeTranslator translator;

  @BeforeEach
  public void setUp() {
    translator = new DSFMediaTypeTranslator();
  }

  @Test
  public void testTranslate_StructuredQueryMediaType() throws UnsupportedMediaTypeException {
    var dsfMediaType = translator.translate(QueryMediaType.STRUCTURED_QUERY);
    assertEquals("application/json", dsfMediaType);
  }

  @Test
  public void testTranslate_CqlQueryMediaType() throws UnsupportedMediaTypeException {
    var dsfMediaType = translator.translate(QueryMediaType.CQL);
    assertEquals("text/cql", dsfMediaType);
  }

  @Test
  public void testTranslate_FhirQueryMediaType() throws UnsupportedMediaTypeException {
    var dsfMediaType = translator.translate(QueryMediaType.FHIR);
    assertEquals("application/x-fhir-query", dsfMediaType);
  }
}
