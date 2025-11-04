package de.numcodex.feasibility_gui_backend.query.api.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.erosb.jsonsKema.JsonParser;
import com.github.erosb.jsonsKema.Schema;
import com.github.erosb.jsonsKema.SchemaLoader;
import de.numcodex.feasibility_gui_backend.dse.DseService;
import de.numcodex.feasibility_gui_backend.query.api.DataExtraction;
import de.numcodex.feasibility_gui_backend.terminology.es.CodeableConceptService;
import jakarta.validation.ConstraintValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
@Slf4j
public class DataExtractionValidatorSpringConfig {

  private static final String JSON_SCHEMA = "/de/numcodex/feasibility_gui_backend/query/api/validation/data-extraction-schema.json";

  @Value("${app.enableQueryValidation}")
  private boolean enabled;

  @Bean
  public ConstraintValidator<DataExtractionValidation, DataExtraction> createDataExtractionValidator(
      @Qualifier("dataExtraction") Schema schema,
      CodeableConceptService codeableConceptService,
      DseService dseService) {
    return enabled
        ? new DataExtractionValidator(schema, codeableConceptService, dseService, new ObjectMapper())
        : new DataExtractionPassValidator();
  }

  @Qualifier("dataExtraction")
  @Bean
  public Schema createDataExtractionValidatorJsonSchema() {
    InputStream inputStream = DataExtractionValidator.class.getResourceAsStream(JSON_SCHEMA);
    var jsonSchema = new JsonParser(inputStream).parse();
    return new SchemaLoader(jsonSchema).load();
  }
}
