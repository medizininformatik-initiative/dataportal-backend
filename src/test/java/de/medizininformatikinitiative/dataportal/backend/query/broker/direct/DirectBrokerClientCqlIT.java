package de.medizininformatikinitiative.dataportal.backend.query.broker.direct;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatikinitiative.dataportal.backend.query.broker.QueryDefinitionNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.broker.QueryNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.broker.SiteNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatusListener;
import de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatusUpdate;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.stream.Stream;

import static de.medizininformatikinitiative.dataportal.backend.query.QueryMediaType.CQL;
import static de.medizininformatikinitiative.dataportal.backend.query.collect.QueryStatus.COMPLETED;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NewClassNamingConvention")
class DirectBrokerClientCqlIT {

  private static final int ASYNC_TIMEOUT_WAIT_MS = 2000;
  private static final Long TEST_BACKEND_QUERY_ID = 1L;

  private final GenericContainer<?> blaze = new GenericContainer<>(DockerImageName.parse("samply/blaze:1.1.2"))
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/health").forStatusCodeMatching(c -> c >= 200 && c <= 500))
      .withStartupAttempts(3);
  private final FhirContext fhirContext = FhirContext.forR4();
  DirectBrokerClientCql client;

  @BeforeAll
  void setUp() {
    blaze.start();
    fhirContext.getRestfulClientFactory().setSocketTimeout(200 * 1000);
    IGenericClient fhirClient = fhirContext.newRestfulGenericClient(
        format("http://localhost:%d/fhir", blaze.getFirstMappedPort()));
    FhirConnector fhirConnector = new FhirConnector(fhirClient);
    FhirHelper fhirHelper = new FhirHelper(fhirContext);
    client = new DirectBrokerClientCql(fhirConnector, false, fhirHelper);

    Stream.of(
            new Patient().setGender(AdministrativeGender.MALE),
            new Patient().setGender(AdministrativeGender.MALE),
            new Patient().setGender(AdministrativeGender.FEMALE))
        .forEach((patient) -> fhirClient.create().resource(patient).execute());
  }

  @AfterAll
  void tearDown() {
    blaze.stop();
  }

  @Test
  void testExecuteQueryMale()
      throws QueryNotFoundException, IOException, SiteNotFoundException, QueryDefinitionNotFoundException {
    var brokerQueryId = client.createQuery(TEST_BACKEND_QUERY_ID);
    var cqlString = FhirHelper.getResourceFileAsString("gender-male.cql");
    client.addQueryDefinition(brokerQueryId, CQL, cqlString);

    var statusListener = mock(QueryStatusListener.class);
    client.addQueryStatusListener(statusListener);
    client.publishQuery(brokerQueryId);

    var statusUpdate = QueryStatusUpdate.builder()
        .source(client)
        .brokerQueryId(brokerQueryId)
        .brokerSiteId("1")
        .status(COMPLETED)
        .build();
    verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS)).onClientUpdate(TEST_BACKEND_QUERY_ID, statusUpdate);

    assertThat(client.getResultSiteIds(brokerQueryId)).containsExactly("1");
    assertEquals(2, client.getResultFeasibility(brokerQueryId, "1"));
  }

  @Test
  void testExecuteQueryFemale()
      throws QueryNotFoundException, IOException, SiteNotFoundException, QueryDefinitionNotFoundException {
    var brokerQueryId = client.createQuery(TEST_BACKEND_QUERY_ID);
    var cqlString = FhirHelper.getResourceFileAsString("gender-female.cql");
    client.addQueryDefinition(brokerQueryId, CQL, cqlString);

    var statusListener = mock(QueryStatusListener.class);
    client.addQueryStatusListener(statusListener);
    client.publishQuery(brokerQueryId);

    var statusUpdate = QueryStatusUpdate.builder()
        .source(client)
        .brokerQueryId(brokerQueryId)
        .brokerSiteId("1")
        .status(COMPLETED)
        .build();
    verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS)).onClientUpdate(TEST_BACKEND_QUERY_ID, statusUpdate);

    assertThat(client.getResultSiteIds(brokerQueryId)).containsExactly("1");
    assertEquals(1, client.getResultFeasibility(brokerQueryId, "1"));
  }

}
