package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

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
  public void serialize(UpgradeIssueType upgradeIssueType, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("code", "UPGRADE-" + upgradeIssueType.code());
    jsonGenerator.writeStringField("detail", upgradeIssueType.detail());
    jsonGenerator.writeEndObject();
  }
}
