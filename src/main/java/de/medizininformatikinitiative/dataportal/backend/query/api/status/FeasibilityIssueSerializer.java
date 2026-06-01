package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Custom Serializer for {@link FeasibilityIssue} to add prefix to code and replace boolean with yes/no.
 */
public class FeasibilityIssueSerializer extends StdSerializer<FeasibilityIssue> {

  public FeasibilityIssueSerializer() {
    this(null);
  }

  public FeasibilityIssueSerializer(Class<FeasibilityIssue> t) {
    super(t);
  }

  @Override
  public void serialize(FeasibilityIssue feasibilityIssue, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringProperty("message", feasibilityIssue.message());
    jsonGenerator.writeStringProperty("type", feasibilityIssue.type().value());
    jsonGenerator.writeStringProperty("code", "FEAS-" + feasibilityIssue.code());
    jsonGenerator.writeStringProperty("severity", feasibilityIssue.severity().value());
    jsonGenerator.writeEndObject();
  }
}
