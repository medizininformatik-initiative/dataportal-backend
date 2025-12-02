package de.numcodex.feasibility_gui_backend.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.Crtdl;
import de.numcodex.feasibility_gui_backend.query.api.Dataquery;
import de.numcodex.feasibility_gui_backend.query.api.Ccdl;
import de.numcodex.feasibility_gui_backend.query.api.validation.JsonSchemaValidator;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerSpringConfig;
import de.numcodex.feasibility_gui_backend.query.collect.QueryCollectSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dataquery.*;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchSpringConfig;
import de.numcodex.feasibility_gui_backend.query.persistence.DataqueryRepository;
import de.numcodex.feasibility_gui_backend.query.result.ResultServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslatorSpringConfig;
import de.numcodex.feasibility_gui_backend.terminology.validation.CcdlValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("schedule")
@Import({
    BrokerSpringConfig.class,
    QueryTranslatorSpringConfig.class,
    QueryDispatchSpringConfig.class,
    QueryCollectSpringConfig.class,
    QueryHandlerService.class,
    ResultServiceSpringConfig.class,
    DataquerySpringConfig.class
})
@DataJpaTest(
    properties = {
        "app.cqlTranslationEnabled=false",
        "app.fhirTranslationEnabled=false",
        "app.broker.mock.enabled=true",
        "app.broker.direct.enabled=false",
        "app.broker.aktin.enabled=false",
        "app.broker.dsf.enabled=false"
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DataqueryMaintenanceIT {

  @Autowired
  private DataqueryHandler dataqueryHandler;

  @Autowired
  private DataqueryRepository dataqueryRepository;

  private DataqueryMaintenance dataqueryMaintenance;

  @MockitoBean
  private CcdlValidation ccdlValidation;

  @MockitoBean
  private JsonSchemaValidator jsonSchemaValidator;

  @MockitoBean
  private DataqueryCsvExportService dataqueryCsvExportService;

  @Autowired
  @Qualifier("translation")
  private ObjectMapper jsonUtil;

  @BeforeEach
  public void setUp() {
    dataqueryMaintenance = new DataqueryMaintenance(dataqueryRepository);
  }

  @Test
  public void testPurgeExpiredDataquery() throws DataqueryStorageFullException, DataqueryException, InterruptedException {
    var testDataquery = createDataquery();

    dataqueryHandler.storeExpiringDataquery(testDataquery, "test", "-PT2S");
    dataqueryHandler.storeExpiringDataquery(testDataquery, "test", "PT10M");
    assertThat(dataqueryRepository.count()).isEqualTo(2);
    dataqueryMaintenance.purgeExpiredDataqueries();
    assertThat(dataqueryRepository.count()).isOne();
  }

  private Dataquery createDataquery() {
    return Dataquery.builder()
        .label("test")
        .comment("test")
        .content(createCrtdl())
        .resultSize(null)
        .build();
  }

  private Crtdl createCrtdl() {
    return Crtdl.builder()
        .cohortDefinition(createValidCcdl())
        .display("foo")
        .build();
  }

  private Ccdl createValidCcdl() {
    var termCode = TermCode.builder()
        .code("LL2191-6")
        .system("http://loinc.org")
        .display("Geschlecht")
        .build();
    var inclusionCriterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .attributeFilters(List.of())
        .build();
    return Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(inclusionCriterion)))
        .exclusionCriteria(null)
        .display("foo")
        .build();
  }
}
