package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("query")
@Tag("validation")
class ValidationIssueSerializerTest {

  private final ObjectMapper objectMapper = JsonMapper.builderWithJackson2Defaults().build();

  @ParameterizedTest
  @EnumSource(ValidationIssueType.class)
  void testSerialize_prefixesCodeAndWritesDetail(ValidationIssueType validationIssueType) {
    var json = objectMapper.writeValueAsString(validationIssueType);

    var node = objectMapper.readTree(json);
    assertEquals("VAL-" + validationIssueType.code(), node.get("code").asString());
    assertEquals(validationIssueType.detail(), node.get("detail").asString());
  }
}
