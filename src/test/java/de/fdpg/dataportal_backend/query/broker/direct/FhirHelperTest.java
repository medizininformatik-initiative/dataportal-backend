package de.fdpg.dataportal_backend.query.broker.direct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import ca.uhn.fhir.context.FhirContext;
import java.nio.charset.StandardCharsets;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FhirHelperTest {

  private static final String LIBRARY_URI = "uri:1-library-example-uri";
  private static final String MEASURE_URI = "uri:1-measure-example-uri";
  private static final String CQL_STRING = "cql-string";

  FhirHelper fhirHelper;

  @BeforeEach
  void setUp() {

    FhirContext fhirContext = FhirContext.forR4();
    this.fhirHelper = new FhirHelper(fhirContext);
  }

  @Test
  void testCreateBundleSuccess() throws Exception{
    Bundle bundle = fhirHelper.createBundle(CQL_STRING, LIBRARY_URI, MEASURE_URI);

    Library library = (Library) bundle.getEntry().get(0).getResource();
    Measure measure = (Measure) bundle.getEntry().get(1).getResource();
    String s = new String(library.getContent().get(0).getData(), StandardCharsets.UTF_8);
    assertEquals(LIBRARY_URI, library.getUrl());
    assertEquals(MEASURE_URI, measure.getUrl());
    assertEquals(LIBRARY_URI, measure.getLibrary().get(0).asStringValue());
    assertEquals(CQL_STRING, s);
  }

}
