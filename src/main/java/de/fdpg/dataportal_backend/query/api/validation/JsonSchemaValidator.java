package de.fdpg.dataportal_backend.query.api.validation;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import com.networknt.schema.Error;
import com.networknt.schema.regex.JoniRegularExpressionFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JsonSchemaValidator {

  public static final String SCHEMA_CRTDL = "crtdl";
  public static final String SCHEMA_CCDL = "ccdl";
  public static final String SCHEMA_DATAEXTRACTION = "dataExtraction";
  private final Map<String, Schema> schemaCache = new ConcurrentHashMap<>();

  public JsonSchemaValidator() {
    loadSchema(SCHEMA_CRTDL, JsonSchemaIdResolver.SCHEMA_IRI_CRTDL_SCHEMA);
    loadSchema(SCHEMA_CCDL, JsonSchemaIdResolver.SCHEMA_IRI_CCDL_SCHEMA);
    loadSubSchema(SCHEMA_DATAEXTRACTION, "dataExtraction", JsonSchemaIdResolver.SCHEMA_IRI_CRTDL_SCHEMA);
  }

  // only used in tests
  JsonSchemaValidator(Map<String, Schema> testSchemas) {
    schemaCache.putAll(testSchemas);
  }

  private void loadSchema(String name, String schemaIri) {
    SchemaRegistryConfig schemaRegistryConfig = SchemaRegistryConfig.builder()
        .regularExpressionFactory(JoniRegularExpressionFactory.getInstance()).build();
    SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12,
        builder -> builder.schemaRegistryConfig(schemaRegistryConfig)
            .schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers
                .add(new JsonSchemaIdResolver())));

    schemaCache.put(name, schemaRegistry.getSchema(SchemaLocation.of(schemaIri)));
  }

  private void loadSubSchema(String name, String nodeName, String schemaIri) {
    SchemaRegistryConfig schemaRegistryConfig = SchemaRegistryConfig.builder()
        .regularExpressionFactory(JoniRegularExpressionFactory.getInstance()).build();
    SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12,
        builder -> builder.schemaRegistryConfig(schemaRegistryConfig)
            .schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers
                .add(new JsonSchemaIdResolver())));

    var schema = schemaRegistry.getSchema(SchemaLocation.of(schemaIri));
    var subNode = schema.getSchemaNode().get("properties").get(nodeName);

    schemaCache.put(name, schemaRegistry.getSchema(subNode));
  }

  public List<Error> validate(String schemaKey, JsonNode jsonNode) {
    Schema schema = schemaCache.get(schemaKey);
    if (schema == null) {
      throw new IllegalArgumentException("Unknown schema: " + schemaKey);
    }

    return schema.validate(jsonNode,executionContext -> executionContext
        .executionConfig(executionConfig -> executionConfig.formatAssertionsEnabled(true)));
  }
}