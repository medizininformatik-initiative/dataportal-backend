package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Custom Serializer for {@link UpgradeIssueType} to add prefix to code and replace boolean with yes/no.
 */
public class UpgradeIssueSerializer extends StdSerializer<UpgradeIssueType> {

  public UpgradeIssueSerializer() {
    this(null);
  }

  public UpgradeIssueSerializer(Class<UpgradeIssueType> t) {
    super(t);
  }

  @Override
  public void serialize(UpgradeIssueType upgradeIssueType, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringProperty("code", "UPGRADE-" + upgradeIssueType.code());
    jsonGenerator.writeStringProperty("detail", upgradeIssueType.detail());
    jsonGenerator.writeEndObject();
  }
}
