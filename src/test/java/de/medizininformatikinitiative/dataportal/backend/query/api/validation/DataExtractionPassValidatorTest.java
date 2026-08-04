package de.medizininformatikinitiative.dataportal.backend.query.api.validation;

import de.medizininformatikinitiative.dataportal.backend.query.api.Attribute;
import de.medizininformatikinitiative.dataportal.backend.query.api.AttributeGroup;
import de.medizininformatikinitiative.dataportal.backend.query.api.DataExtraction;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("query")
@Tag("api")
@Tag("validation")
@ExtendWith(MockitoExtension.class)
class DataExtractionPassValidatorTest {

  @Spy
  private DataExtractionPassValidator validator;

  @Mock
  private ConstraintValidatorContext ctx;

  @Test
  void testIsValid_populatedDataExtractionPasses() {
    var attribute = Attribute.builder().attributeRef("Patient.active").mustHave(false).build();
    var attributeGroup = AttributeGroup.builder().id("group-1").attributes(List.of(attribute)).build();
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(attributeGroup)).build();

    var validationResult = assertDoesNotThrow(() -> validator.isValid(dataExtraction, ctx));
    assertTrue(validationResult);
  }

  @Test
  void testIsValid_emptyDataExtractionPasses() {
    var dataExtraction = DataExtraction.builder().build();

    var validationResult = assertDoesNotThrow(() -> validator.isValid(dataExtraction, ctx));
    assertTrue(validationResult);
  }

  @Test
  void testIsValid_nullDataExtractionPasses() {
    var validationResult = assertDoesNotThrow(() -> validator.isValid(null, ctx));
    assertTrue(validationResult);
  }
}
