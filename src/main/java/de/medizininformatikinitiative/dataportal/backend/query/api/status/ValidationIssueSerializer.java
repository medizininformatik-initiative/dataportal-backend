package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Custom Serializer for {@link ValidationIssueType} to add prefix to code and replace boolean with yes/no.
 */
public class ValidationIssueSerializer extends StdSerializer<ValidationIssueType> {

  public ValidationIssueSerializer() {
    this(null);
  }

  public ValidationIssueSerializer(Class<ValidationIssueType> t) {
    super(t);
  }

  @Override
  public void serialize(ValidationIssueType validationIssueType, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringProperty("code", "VAL-" + validationIssueType.code());
    jsonGenerator.writeStringProperty("detail", validationIssueType.detail());
    jsonGenerator.writeEndObject();
  }
}
