package de.fdpg.dataportal_backend.query.api.status;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

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
    public void serialize(FeasibilityIssue feasibilityIssue, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("message", feasibilityIssue.message());
        jsonGenerator.writeStringField("type", feasibilityIssue.type().value());
        jsonGenerator.writeStringField("code", "FEAS-" + feasibilityIssue.code());
        jsonGenerator.writeStringField("severity", feasibilityIssue.severity().value());
        jsonGenerator.writeEndObject();
    }
}
