package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("query")
@Tag("validation")
class UpgradeIssueSerializerTest {

  private final ObjectMapper objectMapper = JsonMapper.builderWithJackson2Defaults().build();

  @ParameterizedTest
  @EnumSource(UpgradeIssueType.class)
  void testSerialize_prefixesCodeAndWritesDetail(UpgradeIssueType upgradeIssueType) {
    var json = objectMapper.writeValueAsString(upgradeIssueType);

    var node = objectMapper.readTree(json);
    assertEquals("UPGRADE-" + upgradeIssueType.code(), node.get("code").asString());
    assertEquals(upgradeIssueType.detail(), node.get("detail").asString());
  }
}
