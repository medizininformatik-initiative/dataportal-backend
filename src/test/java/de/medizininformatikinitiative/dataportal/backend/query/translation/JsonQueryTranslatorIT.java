package de.medizininformatikinitiative.dataportal.backend.query.translation;

import de.medizininformatikinitiative.dataportal.backend.common.api.Criterion;
import de.medizininformatikinitiative.dataportal.backend.common.api.TermCode;
import de.medizininformatikinitiative.dataportal.backend.common.api.Unit;
import de.medizininformatikinitiative.dataportal.backend.query.api.AttributeFilter;
import de.medizininformatikinitiative.dataportal.backend.query.api.StructuredQuery;
import de.medizininformatikinitiative.dataportal.backend.query.api.TimeRestriction;
import de.medizininformatikinitiative.dataportal.backend.query.api.ValueFilter;
import org.json.JSONException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.util.List;

import static de.medizininformatikinitiative.dataportal.backend.common.api.Comparator.GREATER_EQUAL;
import static de.medizininformatikinitiative.dataportal.backend.common.api.Comparator.GREATER_THAN;
import static de.medizininformatikinitiative.dataportal.backend.query.api.ValueFilterType.CONCEPT;
import static de.medizininformatikinitiative.dataportal.backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Tag("query")
@Tag("translation")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = QueryTranslatorSpringConfig.class,
    properties = {
        "app.cqlTranslationEnabled=false",
        "app.fhirTranslationEnabled=false",
        "app.mappingsFile=./ontology/mapping_cql.json",
        "app.conceptTreeFile=./ontology/mapping_tree.json"
    }
)
@SuppressWarnings("NewClassNamingConvention")
// app.cqlTranslationEnabled should be set back to true once sq2cql is updated to new ontology input

public class JsonQueryTranslatorIT {

  @Autowired
  @Qualifier("json")
  private QueryTranslator jsonQueryTranslator;

  @Test
  public void testTranslate() throws JSONException {
    var bodyWeightTermCode = TermCode.builder()
        .code("27113001")
        .system("http://snomed.info/sct")
        .version("v1")
        .display("Body weight (observable entity)")
        .build();
    var kgUnit = Unit.builder()
        .code("kg")
        .display("kilogram")
        .build();
    var bodyWeightValueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_EQUAL)
        .quantityUnit(kgUnit)
        .value(50.0)
        .build();
    var hasBmiGreaterThanFifty = Criterion.builder()
        .termCodes(List.of(bodyWeightTermCode))
        .valueFilter(bodyWeightValueFilter)
        .build();
    var testQuery = StructuredQuery.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .build();

    var translationResult = assertDoesNotThrow(() -> jsonQueryTranslator.translate(testQuery));

    var expectedTranslationResult = """
        {
            "version": "http://to_be_decided.com/draft-2/schema#",
            "inclusionCriteria": [
                [
                    {
                        "termCodes": [
                            {
                                "code": "27113001",
                                "system": "http://snomed.info/sct",
                                "version": "v1",
                                "display": "Body weight (observable entity)"
                            }
                        ],
                        "valueFilter": {
                            "type": "quantity-comparator",
                            "comparator": "ge",
                            "unit": {
                                "code": "kg",
                                "display": "kilogram"
                            },
                            "value": 50.0
                        }
                    }
                ]
            ]
        }
        """;

    JSONAssert.assertEquals(expectedTranslationResult, translationResult, false);
  }

  @Test
  public void testTranslate_SupportsTimeRestrictions() throws JSONException {
    var dementiaTermCode = TermCode.builder()
        .code("F00")
        .system("http://fhir.de/CodeSystem/dimdi/icd-10-gm")
        .display("F00")
        .build();
    var hasDementia = Criterion.builder()
        .termCodes(List.of(dementiaTermCode))
        .build();
    var psychologicalDysfunctionTermCode = TermCode.builder()
        .code("F09")
        .system("http://fhir.de/CodeSystem/dimdi/icd-10-gm")
        .display("F09")
        .build();
    var timeRestriction = new TimeRestriction("2021-10-09", "2021-09-09");
    var hasPsychologicalDysfunction = Criterion.builder()
        .termCodes(List.of(psychologicalDysfunctionTermCode))
        .timeRestriction(timeRestriction)
        .build();
    var testQuery = StructuredQuery.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(hasDementia, hasPsychologicalDysfunction)))
        .build();
    var translationResult = assertDoesNotThrow(() -> jsonQueryTranslator.translate(testQuery));

    var expectedTranslationResult = """
        {
            "version": "http://to_be_decided.com/draft-2/schema#",
            "inclusionCriteria": [
                [
                    {
                        "termCodes": [
                            {
                                "code": "F00",
                                "system": "http://fhir.de/CodeSystem/dimdi/icd-10-gm",
                                "display": "F00"
                            }
                        ]
                    },
                    {
                        "termCodes": [
                            {
                                "code": "F09",
                                "system": "http://fhir.de/CodeSystem/dimdi/icd-10-gm",
                                "display": "F09"
                            }
                        ],
                        "timeRestriction": {
                            "beforeDate": "2021-10-09",
                            "afterDate": "2021-09-09"
                        }
                    }
                ]
            ]
        }
        """;

    JSONAssert.assertEquals(expectedTranslationResult, translationResult, false);
  }

  @Test
  public void testTranslate_SupportsAttributeFilters() throws JSONException {
    var ageTermCode = TermCode.builder()
        .code("30525-0")
        .system("http://loinc.org")
        .display("Alter")
        .build();
    var yearUnit = Unit.builder()
        .code("a")
        .display("Jahr")
        .build();
    var ageValueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_THAN)
        .quantityUnit(yearUnit)
        .value(18.0)
        .build();
    var olderThanEighteen = Criterion.builder()
        .termCodes(List.of(ageTermCode))
        .valueFilter(ageValueFilter)
        .build();

    var bodyTemperatureTermCode = TermCode.builder()
        .code("8310-5")
        .system("http://loinc.org")
        .display("Körpertemperatur")
        .build();
    var axillaryMeasureMethod = TermCode.builder()
        .code("LA9370-3")
        .system("http://loinc.org")
        .display("Axillary")
        .build();
    var method = TermCode.builder()
        .code("method")
        .system("abide")
        .display("method")
        .build();
    var axillaryMeasured = AttributeFilter.builder()
        .type(CONCEPT)
        .selectedConcepts(List.of(axillaryMeasureMethod))
        .attributeCode(method)
        .build();
    var bodyTemperatureAxillaryMeasured = Criterion.builder()
        .termCodes(List.of(bodyTemperatureTermCode))
        .attributeFilters(List.of(axillaryMeasured))
        .build();
    var testQuery = StructuredQuery.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(olderThanEighteen)))
        .exclusionCriteria(List.of(List.of(bodyTemperatureAxillaryMeasured)))
        .build();
    var translationResult = assertDoesNotThrow(() -> jsonQueryTranslator.translate(testQuery));

    var expectedTranslationResult = """
        {
            "version": "http://to_be_decided.com/draft-2/schema#",
            "inclusionCriteria": [
                [
                    {
                        "termCodes": [
                            {
                                "code": "30525-0",
                                "system": "http://loinc.org",
                                "display": "Alter"
                            }
                        ],
                        "valueFilter": {
                            "type": "quantity-comparator",
                            "comparator": "gt",
                            "unit": {
                                "code": "a",
                                "display": "Jahr"
                            },
                            "value": 18.0
                        }
                    }
                ]
            ],
            "exclusionCriteria": [
                [
                    {
                        "termCodes": [
                            {
                                "code": "8310-5",
                                "system": "http://loinc.org",
                                "display": "Körpertemperatur"
                            }
                        ],
                        "attributeFilters": [
                            {
                                "type": "concept",
                                "selectedConcepts": [
                                    {
                                        "code": "LA9370-3",
                                        "system": "http://loinc.org",
                                        "display": "Axillary"
                                    }
                                ],
                                "attributeCode": {
                                    "code": "method",
                                    "system": "abide",
                                    "display": "method"
                                }
                            }
                        ]
                    }
                ]
            ]
        }
        """;

    JSONAssert.assertEquals(expectedTranslationResult, translationResult, false);
  }
}
