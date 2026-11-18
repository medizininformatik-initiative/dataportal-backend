package de.medizininformatikinitiative.dataportal.backend.query.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("query")
@Tag("api")
class DataExtractionSerializationTest {

  private final ObjectMapper jsonUtil = JsonMapper.builderWithJackson2Defaults().build();

  @Test
  void emptyAttributesAreSerialized() {
    var group = AttributeGroup.builder()
        .id("group-1")
        .groupReference(URI.create("https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/PatientPseudonymisiert"))
        .attributes(List.of())
        .build();
    var crtdl = Crtdl.builder()
        .version("http://json-schema.org/to-be-done/schema#")
        .dataExtraction(DataExtraction.builder().attributeGroups(List.of(group)).build())
        .build();

    var json = jsonUtil.valueToTree(crtdl);
    var groupNode = json.get("dataExtraction").get("attributeGroups").get(0);

    assertTrue(groupNode.has("attributes"));
    assertTrue(groupNode.get("attributes").isArray());
    assertTrue(groupNode.get("attributes").isEmpty());
    assertFalse(groupNode.has("filter"));
  }

  @Test
  void emptyAttributeGroupsAreSerialized() {
    var json = jsonUtil.valueToTree(DataExtraction.builder().attributeGroups(List.of()).build());

    assertTrue(json.has("attributeGroups"));
    assertTrue(json.get("attributeGroups").isEmpty());
  }

  @Test
  void mustHaveFalseIsSerialized() {
    var attribute = Attribute.builder().attributeRef("Patient.gender").mustHave(false).build();

    var json = jsonUtil.valueToTree(attribute);

    assertTrue(json.has("mustHave"));
    assertEquals(false, json.get("mustHave").asBoolean());
  }
}
