package de.fdpg.dataportal_backend.query.broker.dsf;

import de.fdpg.dataportal_backend.query.broker.QueryDefinitionNotFoundException;
import de.fdpg.dataportal_backend.query.broker.QueryNotFoundException;
import de.fdpg.dataportal_backend.query.broker.UnsupportedMediaTypeException;
import dev.dsf.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static de.fdpg.dataportal_backend.query.QueryMediaType.CQL;
import static de.fdpg.dataportal_backend.query.QueryMediaType.STRUCTURED_QUERY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DSFQueryManagerTest {

  private static final String ORGANIZATION = "My-ZARS";

  @Captor
  ArgumentCaptor<Bundle> bundleCaptor;

  @Mock
  private FhirWebserviceClient fhirWebserviceClient;

  @Mock
  private FhirWebClientProvider fhirWebClientProvider;

  private DSFQueryManager queryHandler;
  private String unknownQueryId;

  @BeforeEach
  public void setUp() {
    var dsfMediaTypeTranslator = new DSFMediaTypeTranslator();
    this.queryHandler = new DSFQueryManager(fhirWebClientProvider, dsfMediaTypeTranslator, ORGANIZATION);
    this.unknownQueryId = UUID.randomUUID().toString();
  }

  @Test
  public void testCreateQuery_QueryIdsDiffer() {
    var queryA = queryHandler.createQuery();
    var queryB = queryHandler.createQuery();

    assertNotEquals(queryA, queryB);
  }

  @Test
  public void testAddQueryDefinition_QueryNotFound() {
    assertThrows(QueryNotFoundException.class, () -> queryHandler.addQueryDefinition(unknownQueryId, CQL, ""));
  }

  @Test
  public void testAddQueryDefinition_AddingContentForExistingMediaTypeOverwritesExistingContent()
      throws UnsupportedMediaTypeException, QueryNotFoundException, FhirWebClientProvisionException, IOException, QueryDefinitionNotFoundException {
    var queryId = queryHandler.createQuery();
    var finalContent = "{\"foo\":\"bar\"}";
    queryHandler.addQueryDefinition(queryId, STRUCTURED_QUERY, "{}");
    queryHandler.addQueryDefinition(queryId, STRUCTURED_QUERY, finalContent);

    when(fhirWebClientProvider.provideFhirWebserviceClient()).thenReturn(fhirWebserviceClient);
    when(fhirWebserviceClient.getBaseUrl()).thenReturn("http://localhost/fhir");
    when(fhirWebserviceClient.postBundle(bundleCaptor.capture())).thenReturn(null);
    queryHandler.publishQuery(queryId);

    var library = (Library) bundleCaptor.getValue().getEntry().stream().filter(entry -> entry.getResource().fhirType().equals("Library"))
        .findFirst()
        .orElseThrow()
        .getResource();

    assertEquals(1, library.getContent().size());
    assertEquals("application/json", library.getContent().get(0).getContentType());
    assertEquals(finalContent, new String(library.getContent().get(0).getData()));
  }

  @Test
  public void testPublishQuery_QueryNotFound() {
    assertThrows(QueryNotFoundException.class, () -> queryHandler.publishQuery(unknownQueryId));
  }


  @Test
  public void testPublishQuery_QueryHasNoQueryDefinitionYet() {
    var queryId = queryHandler.createQuery();

    QueryDefinitionNotFoundException e = assertThrows(QueryDefinitionNotFoundException.class, () -> queryHandler.publishQuery(queryId));
    assertEquals("Query with ID '" + queryId + "' does not contain any query definitions of a known type.", e.getMessage());
  }

  @Test
  public void testPublishQuery_PublishFailed() throws UnsupportedMediaTypeException, QueryNotFoundException {
    var queryId = queryHandler.createQuery();
    queryHandler.addQueryDefinition(queryId, CQL, "");

    when(fhirWebserviceClient.postBundle(any(Bundle.class))).thenThrow();

    assertThrows(IOException.class, () -> queryHandler.publishQuery(queryId));
  }

  @Test
  public void testPublishQuery()
      throws UnsupportedMediaTypeException, QueryNotFoundException, IOException, FhirWebClientProvisionException, QueryDefinitionNotFoundException {
    var queryId = queryHandler.createQuery();
    queryHandler.addQueryDefinition(queryId, CQL, "");

    when(fhirWebClientProvider.provideFhirWebserviceClient()).thenReturn(fhirWebserviceClient);
    when(fhirWebserviceClient.getBaseUrl()).thenReturn("http://localhost/fhir");
    queryHandler.publishQuery(queryId);

    verify(fhirWebserviceClient).postBundle(bundleCaptor.capture());


    List<BundleEntryComponent> bundleEntries = bundleCaptor.getValue().getEntry();
    var task = (Task) bundleEntries.stream().filter(e -> e.getResource().fhirType().equals("Task")).findFirst()
        .orElseThrow().getResource();
    var businessKey = task.getInput().stream().filter(i -> i.getType().getCodingFirstRep().getCode().equals("business-key"))
        .findFirst().orElseThrow().getValue().toString();
    var library = (Library) bundleEntries.stream().filter(e -> e.getResource().fhirType().equals("Library"))
        .findFirst().orElseThrow().getResource();
    var measure = (Measure) bundleEntries.stream().filter(e -> e.getResource().fhirType().equals("Measure"))
        .findFirst().orElseThrow().getResource();
    var measureReferenceSystem = task.getInput().stream().filter(i -> i.getType().getCodingFirstRep()
            .getCode().equals("measure-reference"))
        .findFirst().orElseThrow().getType().getCodingFirstRep().getSystem();


    assertEquals(businessKey, queryId);
    assertNotNull(library.getName());
    assertEquals("http://medizininformatik-initiative.de/bpe/Process/feasibilityRequest|1.0",
        task.getInstantiatesCanonical());
    assertEquals(1, task.getMeta().getProfile().stream().filter(p -> p.getValueAsString()
            .equals("http://medizininformatik-initiative.de/fhir/StructureDefinition/feasibility-task-request|1.0"))
        .count());
    assertEquals(1, library.getMeta().getProfile().stream().filter(p -> p.getValueAsString()
            .equals("http://medizininformatik-initiative.de/fhir/StructureDefinition/feasibility-library|1.0"))
        .count());
    assertEquals(1, measure.getMeta().getProfile().stream().filter(p -> p.getValueAsString()
            .equals("http://medizininformatik-initiative.de/fhir/StructureDefinition/feasibility-measure|1.0"))
        .count());
    assertEquals(36, measure.getGroupFirstRep().getPopulationFirstRep().getId().length());
    assertEquals("http://medizininformatik-initiative.de/fhir/CodeSystem/feasibility", measureReferenceSystem);
  }

  @Test
  public void testRemoveQuery_QueryNotFound() {
    assertThrows(QueryNotFoundException.class, () -> queryHandler.removeQuery(unknownQueryId));
  }

  @Test
  public void testRemoveQuery() throws QueryNotFoundException {
    var queryId = queryHandler.createQuery();

    queryHandler.removeQuery(queryId);
    assertThrows(QueryNotFoundException.class, () -> queryHandler.removeQuery(queryId));
  }
}
