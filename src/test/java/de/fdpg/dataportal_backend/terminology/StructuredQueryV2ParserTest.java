package de.fdpg.dataportal_backend.terminology;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fdpg.dataportal_backend.query.api.StructuredQuery;
import org.junit.jupiter.api.Test;

public class StructuredQueryV2ParserTest {

  @Test
  public void queryBuilder() throws JsonProcessingException {

    String testQuery = """
        {
          "version" : "http://to_be_decided.com/draft-2/schema#",
          "inclusionCriteria" : [ [ {
            "termCodes" : [ {
              "code" : "LL2191-6",
              "system" : "http://loinc.org",
              "display" : "Geschlecht"
            } ],
            "valueFilter" : {
              "type" : "concept",
              "selectedConcepts" : [ {
                "code" : "F",
                "system" : "https://fhir.loinc.org/CodeSystem/$lookup?system=http://loinc.org&code=LL2191-6",
                "version" : "",
                "display" : "female"
              }, {
                "code" : "M",
                "system" : "https://fhir.loinc.org/CodeSystem/$lookup?system=http://loinc.org&code=LL2191-6",
                "version" : "",
                "display" : "male"
              } ]
            }
          } ], [ {
            "termCodes" : [ {
              "code" : "30525-0",
              "system" : "http://loinc.org",
              "display" : "Alter"
            } ],
            "valueFilter" : {
              "type" : "quantity-comparator",
              "comparator" : "gt",
              "unit" : {
                "code" : "a",
                "display" : "Jahr"
              },
              "value" : 18.0
            }
          } ], [ {
            "termCodes" : [ {
              "code" : "F00",
              "system" : "http://fhir.de/CodeSystem/dimdi/icd-10-gm",
              "display" : "F00"
            } ]
          }, {
            "termCodes" : [ {
              "code" : "F09",
              "system" : "http://fhir.de/CodeSystem/dimdi/icd-10-gm",
              "display" : "F09"
            } ],
            "timeRestriction" : {
              "beforeDate" : "2021-10-09",
              "afterDate" : "2021-09-09"
            }
          } ] ],
          "exclusionCriteria" : [ [ {
            "termCodes" : [ {
              "code" : "LL2191-6",
              "system" : "http://loinc.org",
              "display" : "Geschlecht"
            } ],
            "valueFilter" : {
              "type" : "concept",
              "selectedConcepts" : [ {
                "code" : "male",
                "system" : "",
                "version" : "",
                "display" : "male"
              } ]
            }
          } ], [ {
            "termCodes" : [ {
              "code" : "30525-0",
              "system" : "http://loinc.org",
              "display" : "Alter"
            } ],
            "valueFilter" : {
              "type" : "quantity-comparator",
              "comparator" : "gt",
              "unit" : {
                "code" : "year",
                "display" : "Jahr"
              },
              "value" : 65.0
            }
          } ], [ {
            "termCodes" : [ {
              "code" : "F00.9",
              "system" : "http://fhir.de/CodeSystem/dimdi/icd-10-gm",
              "display" : "F00.9"
            } ]
          }, {
            "termCodes" : [ {
              "code" : "8310-5",
              "system" : "http://loinc.org",
              "display" : "Körpertemperatur"
            } ],
            "attributeFilters" : [ {
              "type" : "concept",
              "selectedConcepts" : [ {
                "code" : "LA9370-3",
                "system" : "http://loinc.org",
                "display" : "Axillary"
              } ],
              "attributeCode" : {
                "code" : "method",
                "system" : "abide",
                "display" : "method"
              }
            } ],
            "valueFilter" : {
              "type" : "quantity-range",
              "unit" : {
                "code" : "Cel",
                "display" : "°C"
              },
              "minValue" : 35.0,
              "maxValue" : 39.0
            },
            "timeRestriction" : {
              "beforeDate" : "2021-10-09",
              "afterDate" : "2021-09-09"
            }
          } ] ],
          "display" : "Beispiel-Query"
        }""";

    var objectMapper = new ObjectMapper();
    var test = objectMapper.readValue(testQuery, StructuredQuery.class);
    var jsonString = objectMapper.writeValueAsString(test);
    assert (jsonString.contains("attributeCode"));
    assert (jsonString.contains("exclusionCriteria"));
    assert (jsonString.contains("timeRestriction"));
    assert (jsonString.contains("attributeFilters"));

  }

}
