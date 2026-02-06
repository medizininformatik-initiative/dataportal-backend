package de.medizininformatikinitiative.dataportal.backend.query.dataquery;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.medizininformatikinitiative.dataportal.backend.query.api.validation.JsonSchemaValidator;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.DataqueryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataquerySpringConfig {

  @Bean
  public DataqueryHandler createDataqueryHandler(
      DataqueryCsvExportService dataqueryCsvExportService,
      @Qualifier("translation") ObjectMapper jsonUtil,
      DataqueryRepository dataqueryRepository,
      @Value("${app.maxSavedQueriesPerUser}") Integer maxSavedQueriesPerUser,
      @Value("${app.keycloakAdminRole}") String keycloakAdminRole
  ) {
    return new DataqueryHandler(dataqueryCsvExportService, jsonUtil, dataqueryRepository, maxSavedQueriesPerUser, keycloakAdminRole);
  }
}
