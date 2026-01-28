package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

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
  public void serialize(ValidationIssueType validationIssueType, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("code", "VAL-" + validationIssueType.code());
    jsonGenerator.writeStringField("detail", validationIssueType.detail());
    jsonGenerator.writeEndObject();
  }
}
