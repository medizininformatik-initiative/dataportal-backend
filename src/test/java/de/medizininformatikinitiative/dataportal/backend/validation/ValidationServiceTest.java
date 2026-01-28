package de.medizininformatikinitiative.dataportal.backend.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.Error;
import com.networknt.schema.path.NodePath;
import com.networknt.schema.path.PathType;
import de.medizininformatikinitiative.dataportal.backend.query.api.Ccdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.Crtdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.DataExtraction;
import de.medizininformatikinitiative.dataportal.backend.query.api.Dataquery;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssue;
import de.medizininformatikinitiative.dataportal.backend.query.api.validation.JsonSchemaValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

  @Mock
  private SmartValidator validator;

  @Mock
  private JsonSchemaValidator jsonSchemaValidator;

  @Spy
  private ObjectMapper jsonUtil = new ObjectMapper();

  private ValidationService validationService;

  private ValidationService createValidationService() {
    return new ValidationService(validator, jsonSchemaValidator,  jsonUtil);
  }

  @BeforeEach
  void setUp() {
    Mockito.reset(validator, jsonSchemaValidator, jsonUtil);
    validationService = createValidationService();
  }

  @Test
  void isValidCcdl_true() throws JsonProcessingException {
    doNothing().when(validator).validate(any(Ccdl.class), any(BindingResult.class));

    var result = validationService.isValid(createDataquery().content().cohortDefinition());

    Assertions.assertTrue(result);
  }

  @Test
  void isValidCcdl_false() throws JsonProcessingException {
    doAnswer(invocation -> {
      BindingResult bindingResult = invocation.getArgument(1);
      bindingResult.rejectValue("display", "error.code", "Invalid value");
      return null;
    }).when(validator).validate(any(Ccdl.class), any(BindingResult.class));

    var result = validationService.isValid(createDataquery().content().cohortDefinition());

    Assertions.assertFalse(result);
  }

  @Test
  void testIsValidCrtdl_true() throws JsonProcessingException {
    doNothing().when(validator).validate(any(Crtdl.class), any(BindingResult.class));

    var result = validationService.isValid(createDataquery().content());

    Assertions.assertTrue(result);
  }

  @Test
  void testIsValidCrtdl_false() throws JsonProcessingException {
    doAnswer(invocation -> {
      BindingResult bindingResult = invocation.getArgument(1);
      bindingResult.rejectValue("display", "error.code", "Invalid value");
      return null;
    }).when(validator).validate(any(Crtdl.class), any(BindingResult.class));

    var result = validationService.isValid(createDataquery().content());

    Assertions.assertFalse(result);
  }

  @Test
  void testIsValidDataquery_true() throws JsonProcessingException {
    doNothing().when(validator).validate(any(Dataquery.class), any(BindingResult.class));

    var result = validationService.isValid(createDataquery());

    Assertions.assertTrue(result);
  }

  @Test
  void testIsValidDataquery_false() throws JsonProcessingException {
    doAnswer(invocation -> {
      BindingResult bindingResult = invocation.getArgument(1);
      bindingResult.rejectValue("label", "error.code", "Invalid value");
      return null;
    }).when(validator).validate(any(Dataquery.class), any(BindingResult.class));

    var result = validationService.isValid(createDataquery());

    Assertions.assertFalse(result);
  }

  @Test
  void testIsValidDataExtraction_true() throws JsonProcessingException {
    doNothing().when(validator).validate(any(DataExtraction.class), any(BindingResult.class));

    var result = validationService.isValid(createDataquery().content().dataExtraction());

    Assertions.assertTrue(result);
  }

  @Test
  void testIsValidDataExtraction_false() throws JsonProcessingException {
    doAnswer(invocation -> {
      BindingResult bindingResult = invocation.getArgument(1);
      bindingResult.rejectValue("attributeGroups", "error.code", "Invalid value");
      return null;
    }).when(validator).validate(any(DataExtraction.class), any(BindingResult.class));

    var result = validationService.isValid(createDataquery().content().dataExtraction());

    Assertions.assertFalse(result);
  }

  @Test
  void validateDataExtractionContent_doesNotThrow() {
    doNothing().when(validator).validate(any(DataExtraction.class), any(BindingResult.class));

    assertDoesNotThrow(() -> validationService.validateDataExtractionContent(createDataquery().content().dataExtraction()));
  }

  @Test
  void validateDataExtractionContent_throwsOnError() {
    doAnswer(invocation -> {
      BindingResult bindingResult = invocation.getArgument(1);
      bindingResult.rejectValue("attributeGroups", "error.code", "Invalid value");
      return null;
    }).when(validator).validate(any(DataExtraction.class), any(BindingResult.class));

    assertThrows(MethodArgumentNotValidException.class, () ->
        validationService.validateDataExtractionContent(createDataquery().content().dataExtraction())
    );
  }

  @Test
  void validateCcdlContent_doesNotThrow() {
    doNothing().when(validator).validate(any(Ccdl.class), any(BindingResult.class));

    assertDoesNotThrow(() -> validationService.validateCcdlContent(createDataquery().content().cohortDefinition()));
  }

  @Test
  void validateCcdlContent_throwsOnError() {
    doAnswer(invocation -> {
      BindingResult bindingResult = invocation.getArgument(1);
      bindingResult.rejectValue("display", "error.code", "Invalid value");
      return null;
    }).when(validator).validate(any(Ccdl.class), any(BindingResult.class));

    assertThrows(MethodArgumentNotValidException.class, () ->
        validationService.validateCcdlContent(createDataquery().content().cohortDefinition())
    );
  }

  @Test
  void validateCrtdlContent_doesNotThrow() {
    doNothing().when(validator).validate(any(Crtdl.class), any(BindingResult.class));

    assertDoesNotThrow(() -> validationService.validateCrtdlContent(createDataquery().content()));
  }

  @Test
  void validateCrtdlContent_throwsOnError() {
    doAnswer(invocation -> {
      BindingResult bindingResult = invocation.getArgument(1);
      bindingResult.rejectValue("display", "error.code", "Invalid value");
      return null;
    }).when(validator).validate(any(Crtdl.class), any(BindingResult.class));

    assertThrows(MethodArgumentNotValidException.class, () ->
        validationService.validateCrtdlContent(createDataquery().content())
    );
  }

  @Test
  void validateDataqueryContent_doesNotThrow() {
    doNothing().when(validator).validate(any(Dataquery.class), any(BindingResult.class));

    assertDoesNotThrow(() -> validationService.validateDataqueryContent(createDataquery()));
  }

  @Test
  void validateDataqueryContent_throwsOnError() {
    doAnswer(invocation -> {
      BindingResult bindingResult = invocation.getArgument(1);
      bindingResult.rejectValue("label", "error.code", "Invalid value");
      return null;
    }).when(validator).validate(any(Dataquery.class), any(BindingResult.class));

    assertThrows(MethodArgumentNotValidException.class, () ->
        validationService.validateDataqueryContent(createDataquery())
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
      JsonSchemaValidator.SCHEMA_CRTDL,
      JsonSchemaValidator.SCHEMA_CCDL,
      JsonSchemaValidator.SCHEMA_DATAEXTRACTION,
      JsonSchemaValidator.SCHEMA_DATAQUERY
  })
  void validateSchema_succeedsNoErrors(String value) {
    doReturn(List.of()).when(jsonSchemaValidator).validate(any(String.class), any(JsonNode.class));

    List<ValidationIssue> issueList = switch (value) {
      case JsonSchemaValidator.SCHEMA_CRTDL ->
          assertDoesNotThrow(() -> validationService.validateCrtdlSchema(createCrtdlJsonNode()));
      case JsonSchemaValidator.SCHEMA_CCDL ->
          assertDoesNotThrow(() -> validationService.validateCcdlSchema(createCcdlJsonNode()));
      case JsonSchemaValidator.SCHEMA_DATAEXTRACTION ->
          assertDoesNotThrow(() -> validationService.validateDataExtractionSchema(createDataExtractionJsonNode()));
      case JsonSchemaValidator.SCHEMA_DATAQUERY ->
          assertDoesNotThrow(() -> validationService.validateDataquerySchema(createDataqueryJsonNode()));
      default -> null;
    };

    assertThat(issueList).isInstanceOf(List.class);
    assertThat(issueList).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      JsonSchemaValidator.SCHEMA_CRTDL,
      JsonSchemaValidator.SCHEMA_CCDL,
      JsonSchemaValidator.SCHEMA_DATAEXTRACTION,
      JsonSchemaValidator.SCHEMA_DATAQUERY
  })
  void validateSchema_succeedsWithErrors(String value) {
    doReturn(List.of(
        Error.builder()
            .message("something went wrong")
            .instanceLocation(
                new NodePath(PathType.DEFAULT)
            )
            .build()
    )).when(jsonSchemaValidator).validate(any(String.class), any(JsonNode.class));

    List<ValidationIssue> issueList = switch (value) {
      case JsonSchemaValidator.SCHEMA_CRTDL ->
          assertDoesNotThrow(() -> validationService.validateCrtdlSchema(createCrtdlJsonNode()));
      case JsonSchemaValidator.SCHEMA_CCDL ->
          assertDoesNotThrow(() -> validationService.validateCcdlSchema(createCcdlJsonNode()));
      case JsonSchemaValidator.SCHEMA_DATAEXTRACTION ->
          assertDoesNotThrow(() -> validationService.validateDataExtractionSchema(createDataExtractionJsonNode()));
      case JsonSchemaValidator.SCHEMA_DATAQUERY ->
          assertDoesNotThrow(() -> validationService.validateDataquerySchema(createDataqueryJsonNode()));
      default -> null;
    };

    assertThat(issueList).isInstanceOf(List.class);
    assertThat(issueList).isNotEmpty();
    assertThat(issueList.get(0)).isInstanceOf(ValidationIssue.class);
    assertThat(issueList.get(0).value().code()).startsWith(("VALIDATION-"));
  }

  @Test
  void ccdlFromJsonNode_succeeds() {
    var ccdl = assertDoesNotThrow(() -> validationService.ccdlFromJsonNode(createCcdlJsonNode()));

    assertThat(ccdl).isInstanceOf(Ccdl.class);
  }

  @Test
  void crtdlFromJsonNode_succeeds() {
    var crtdl = assertDoesNotThrow(() -> validationService.crtdlFromJsonNode(createCrtdlJsonNode()));

    assertThat(crtdl).isInstanceOf(Crtdl.class);
  }

  @Test
  void dataqueryFromJsonNode_succeeds() {
    var dataquery = assertDoesNotThrow(() -> validationService.dataqueryFromJsonNode(createDataqueryJsonNode()));

    assertThat(dataquery).isInstanceOf(Dataquery.class);
  }

  @Test
  void dataExtractionFromJsonNode_succeeds() {
    var dataExtraction = assertDoesNotThrow(() -> validationService.dataExtractionFromJsonNode(createDataExtractionJsonNode()));

    assertThat(dataExtraction).isInstanceOf(DataExtraction.class);
  }

  @Test
  void dataExtractionFromJsonNode_throwsOnInvalid() {
    assertThrows(IllegalArgumentException.class, () -> validationService.dataExtractionFromJsonNode(JsonNodeFactory.instance.objectNode()));
  }

  private JsonNode createDataExtractionJsonNode() throws JsonProcessingException {
    return createCrtdlJsonNode().get("dataExtraction");
  }

  private JsonNode createCcdlJsonNode() throws JsonProcessingException {
    return createCrtdlJsonNode().get("cohortDefinition");
  }

  private JsonNode createCrtdlJsonNode() throws JsonProcessingException {
    return createDataqueryJsonNode().get("content");
  }

  private JsonNode createDataqueryJsonNode() throws JsonProcessingException {
    String json = """
        {
          "label": "example crtdl full",
          "comment": "this can be a longer text explaining what this query is for",
          "content": {
            "display": "",
            "version": "http://json-schema.org/to-be-done/schema#",
            "cohortDefinition": {
              "version": "http://to_be_decided.com/draft-1/schema#",
              "display": "Ausgewählte Merkmale",
              "inclusionCriteria": [
                [
                  {
                    "termCodes": [
                      {
                        "code": "263495000",
                        "display": "Geschlecht",
                        "system": "http://snomed.info/sct",
                        "version": ""
                      },
                      {
                        "code": "invalid-code-correct-context",
                        "display": "Geschlecht",
                        "system": "http://snomed.info/sct",
                        "version": ""
                      }
                    ],
                    "context": {
                      "code": "Patient",
                      "display": "Patient",
                      "system": "fdpg.mii.cds",
                      "version": "1.0.0"
                    },
                    "valueFilter": {
                      "selectedConcepts": [
                        {
                          "code": "female",
                          "display": "Female",
                          "system": "http://hl7.org/fhir/administrative-gender",
                          "version": "2099"
                        },
                        {
                          "code": "other",
                          "display": "Other",
                          "system": "http://hl7.org/fhir/administrative-gender",
                          "version": "2099"
                        }
                      ],
                      "type": "concept"
                    }
                  },
                  {
                    "termCodes": [
                      {
                        "code": "263495000",
                        "display": "Geschlecht",
                        "system": "http://snomed.info/sct",
                        "version": ""
                      },
                      {
                        "code": "invalid-code-correct-context",
                        "display": "Geschlecht",
                        "system": "http://snomed.info/sct",
                        "version": ""
                      }
                    ],
                    "context": {
                      "code": "invalid-context",
                      "display": "Patient",
                      "system": "fdpg.mii.cds",
                      "version": "1.0.0"
                    },
                    "valueFilter": {
                      "selectedConcepts": [
                        {
                          "code": "female",
                          "display": "Female",
                          "system": "http://hl7.org/fhir/administrative-gender",
                          "version": "2099"
                        },
                        {
                          "code": "other",
                          "display": "Other",
                          "system": "http://hl7.org/fhir/administrative-gender",
                          "version": "2099"
                        }
                      ],
                      "type": "concept"
                    }
                  }
                ],
                [
                  {
                    "attributeFilters": [
                      {
                        "type": "reference",
                        "criteria": [
                          {
                            "termCodes": [
                              {
                                "code": "invalid-code-123-in-ref- currently i am not flagged",
                                "display": "Diabetes mellitus",
                                "system": "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
                                "version": "2025"
                              }
                            ],
                            "context": {
                              "code": "Diagnose",
                              "display": "Diagnose",
                              "system": "fdpg.mii.cds",
                              "version": "1.0.0"
                            }
                          }
                        ],
                        "attributeCode": {
                          "code": "festgestellteDiagnose",
                          "display": "Festgestellte Diagnose",
                          "system": "http://hl7.org/fhir/StructureDefinition"
                        }
                      }
                    ],
                    "termCodes": [
                      {
                        "code": "119297000",
                        "display": "Blood specimen",
                        "system": "http://snomed.info/sct",
                        "version": "http://snomed.info/sct/900000000000207008/version/20250701"
                      }
                    ],
                    "context": {
                      "code": "Specimen",
                      "display": "Bioprobe",
                      "system": "fdpg.mii.cds",
                      "version": "1.0.0"
                    },
                    "timeRestriction": {
                      "afterDate": "2025-10-14",
                      "beforeDate": "2025-10-18"
                    }
                  },
                  {
                    "termCodes": [
                      {
                        "code": "718-7",
                        "display": "Hemoglobin [Mass/volume] in Blood",
                        "system": "http://loinc.org",
                        "version": "2.80"
                      }
                    ],
                    "context": {
                      "code": "Laboruntersuchung",
                      "display": "Laboruntersuchung",
                      "system": "fdpg.mii.cds",
                      "version": "1.0.0"
                    }
                  }
                ]
              ]
            },
            "dataExtraction": {
              "attributeGroups": [
                {
                  "attributes": [
                    {
                      "attributeRef": "Patient.active",
                      "mustHave": false
                    },
                    {
                      "attributeRef": "Patient.deceased[x]",
                      "mustHave": false
                    },
                    {
                      "attributeRef": "Patient.address:Strassenanschrift.country",
                      "mustHave": false
                    }
                  ],
                  "id": "87dc6b3b-0737-409c-acd2-75c6d10ebbce",
                  "groupReference": "https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/PatientPseudonymisiert",
                  "name": "MII PR Person Patient (Pseudonymisiert)"
                }
              ]
            }
          }
        }
        """;

    return jsonUtil.readTree(json);
  }

  private Dataquery createDataquery() throws JsonProcessingException {
    return validationService.dataqueryFromJsonNode(createDataqueryJsonNode());
  }
}