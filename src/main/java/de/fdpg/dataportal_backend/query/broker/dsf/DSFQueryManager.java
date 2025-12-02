package de.fdpg.dataportal_backend.query.broker.dsf;

import de.fdpg.dataportal_backend.query.QueryMediaType;
import de.fdpg.dataportal_backend.query.broker.QueryDefinitionNotFoundException;
import de.fdpg.dataportal_backend.query.broker.QueryNotFoundException;
import de.fdpg.dataportal_backend.query.broker.UnsupportedMediaTypeException;
import dev.dsf.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST;
import static org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE;
import static org.hl7.fhir.r4.model.Task.TaskIntent.ORDER;
import static org.hl7.fhir.r4.model.Task.TaskStatus.REQUESTED;

/**
 * Manager for feasibility queries.
 * <p>
 * Can be used to set up queries and publish them.
 */
class DSFQueryManager implements QueryManager {

  private static final String INSTANTIATE_CANONICAL = "http://medizininformatik-initiative.de/bpe/Process/feasibilityRequest|1.0";
  private static final String REQUEST_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/feasibility-task-request|1.0";
  private static final String MEASURE_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/feasibility-measure|1.0";
  private static final String LIBRARY_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/feasibility-library|1.0";
  private static final String REQUEST_URL_TASK = "Task";
  private static final String REQUEST_URL_LIBRARY = "Library";
  private static final String REQUEST_URL_MEASURE = "Measure";
  private static final String MEASURE_CRITERIA_LANGUAGE = "text/cql";
  private static final String MEASURE_CRITERIA_EXPRESSION = "InInitialPopulation";
  private static final String BPMN_FEASIBILITY_REQUEST_MESSAGE = "feasibilityRequestMessage";
  private static final String REQUESTER_TYPE = "Organization";
  private static final String RECIPIENT_TYPE = "Organization";

  private static final String LIBRARY_NAME = "Retrieve";
  private static final String LIBRARY_VERSION = "1.0.0";

  private static final String CODE_SYSTEM_FEASIBILITY = "http://medizininformatik-initiative.de/fhir/CodeSystem/feasibility";
  private static final String CODE_SYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE = "measure-reference";
  private static final String CODE_SYSTEM_ORGANIZATION = "http://dsf.dev/sid/organization-identifier";
  private static final String CODE_SYSTEM_BPMN_MESSAGE = "http://dsf.dev/fhir/CodeSystem/bpmn-message";
  private static final String CODE_SYSTEM_BPMN_MESSAGE_VALUE_MESSAGE_NAME = "message-name";
  private static final String CODE_SYSTEM_BPMN_MESSAGE_VALUE_BUSINESS_KEY = "business-key";
  private static final String CODE_SYSTEM_LIBRARY_TYPE = "http://terminology.hl7.org/CodeSystem/library-type";
  private static final String CODE_SYSTEM_LIBRARY_TYPE_VALUE_LOGIC_LIBRARY = "logic-library";
  private static final String CODE_SYSTEM_READ_ACCESS_TAG = "http://dsf.dev/fhir/CodeSystem/read-access-tag";
  private static final String CODE_SYSTEM_READ_ACCESS_TAG_VALUE_ALL = "ALL";
  private static final String CODE_SYSTEM_MEASURE_SCORING = "http://terminology.hl7.org/CodeSystem/measure-scoring";
  private static final String CODE_SYSTEM_MEASURE_SCORING_VALUE_COHORT = "cohort";
  private static final String CODE_SYSTEM_MEASURE_POPULATION = "http://terminology.hl7.org/CodeSystem/measure-population";
  private static final String CODE_SYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION = "initial-population";

  private final FhirWebClientProvider fhirWebClientProvider;
  private final DSFMediaTypeTranslator mediaTypeTranslator;
  private final String organizationId;
  private final Map<String, DSFQueryData> queryHeap;
  private FhirWebserviceClient fhirWebserviceClient;

  /**
   * Creates a new {@link DSFQueryManager} instance.
   *
   * @param fhirWebClientProvider Provider capable of providing a client to communicate with a FHIR server via HTTP.
   * @param mediaTypeTranslator   Translates different media types so that they can be sent to the ZARS.
   * @param organizationId        Identifies the local FHIR server instance (ZARS) that queries get published to.
   */
  DSFQueryManager(FhirWebClientProvider fhirWebClientProvider, DSFMediaTypeTranslator mediaTypeTranslator,
                  String organizationId) {
    this.fhirWebClientProvider = fhirWebClientProvider;
    this.mediaTypeTranslator = mediaTypeTranslator;
    this.organizationId = organizationId;
    this.queryHeap = new HashMap<>();
  }

  @Override
  public String createQuery() {
    var queryId = UUID.randomUUID().toString();
    queryHeap.put(queryId, new DSFQueryData());

    return queryId;
  }

  @Override
  public void addQueryDefinition(String queryId, QueryMediaType queryMediaType, String content) throws QueryNotFoundException,
      UnsupportedMediaTypeException {
    var translatedMediaType = mediaTypeTranslator.translate(queryMediaType);

    var query = queryHeap.get(queryId);
    if (query == null) {
      throw new QueryNotFoundException(queryId);
    }

    query.addQueryContent(translatedMediaType, content);
  }

  @Override
  public void publishQuery(String queryId) throws QueryNotFoundException, QueryDefinitionNotFoundException, IOException {
    var query = queryHeap.get(queryId);
    if (query == null) {
      throw new QueryNotFoundException(queryId);
    }

    var queryContents = query.getContentByType();
    if (queryContents.isEmpty()) {
      throw new QueryDefinitionNotFoundException(queryId);
    }

    if (fhirWebserviceClient == null) {
      try {
        fhirWebserviceClient = fhirWebClientProvider.provideFhirWebserviceClient();
      } catch (Exception e) {
        throw new IOException("could not provide fhir webservice client", e);
      }
    }

    var queryBundle = createQueryBundleWithTask(queryId);
    var libraryId = UUID.randomUUID();
    queryBundle = addLibrary(queryBundle, queryContents, libraryId);
    queryBundle = addMeasure(queryBundle, libraryId);

    try {
      fhirWebserviceClient.postBundle(queryBundle);
    } catch (Exception e) {
      throw new IOException("Unable to publish query with ID " + queryId, e);
    }
  }

  @Override
  public void removeQuery(String queryId) throws QueryNotFoundException {
    if (!queryHeap.containsKey(queryId)) {
      throw new QueryNotFoundException(queryId);
    }
    queryHeap.remove(queryId);
  }

  /**
   * Creates a canonical url for a {@link UUID} to be used in a FHIR context.
   *
   * @param uuid The UUID to be used.
   * @return The canonical url for the given UUID.
   */
  private String createCanonicalUUIDUrn(UUID uuid) {
    return "urn:uuid:" + uuid;
  }

  /**
   * Creates a query bundle with a task attached to it.
   * <p>
   * The task will make sure that the query gets distributed within the DSF context.
   *
   * @param queryId Identifies the query for later use.
   * @return A {@link Bundle} with a task attached to it.
   */
  private Bundle createQueryBundleWithTask(String queryId) {
    Bundle queryBundle = new Bundle().setType(TRANSACTION);

    Task task = new Task()
        .setStatus(REQUESTED)
        .setIntent(ORDER)
        .setAuthoredOn(new Date())
        .setInstantiatesCanonical(INSTANTIATE_CANONICAL);

    task.getRequester()
        .setType(REQUESTER_TYPE)
        .getIdentifier().setSystem(CODE_SYSTEM_ORGANIZATION).setValue(organizationId);

    task.getRestriction().getRecipientFirstRep()
        .setType(RECIPIENT_TYPE)
        .getIdentifier().setSystem(CODE_SYSTEM_ORGANIZATION).setValue(organizationId);

    task.addInput()
        .setType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(CODE_SYSTEM_BPMN_MESSAGE)
                .setCode(CODE_SYSTEM_BPMN_MESSAGE_VALUE_MESSAGE_NAME)))
        .setValue(new StringType(BPMN_FEASIBILITY_REQUEST_MESSAGE));
    task.addInput()
        .setType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(CODE_SYSTEM_BPMN_MESSAGE)
                .setCode(CODE_SYSTEM_BPMN_MESSAGE_VALUE_BUSINESS_KEY)))
        .setValue(new StringType(queryId));
    task.setMeta(new Meta().addProfile(REQUEST_PROFILE));

    queryBundle.addEntry()
        .setRequest(new BundleEntryRequestComponent()
            .setMethod(POST)
            .setUrl(REQUEST_URL_TASK))
        .setResource(task)
        .setFullUrl(createCanonicalUUIDUrn(UUID.randomUUID()));

    return queryBundle;
  }

  /**
   * Given a query bundle adds a library resource to it.
   * <p>
   * Adds the library with the given content associated with it using the given media type.
   *
   * @param queryBundle   The library is added to this query bundle.
   * @param queryContents Contents of the library mapped by their corresponding media types.
   * @param libraryId     Identifies the library for referential usage.
   * @return A copy of the given {@link Bundle} with the library added.
   */
  private Bundle addLibrary(Bundle queryBundle, Map<String, String> queryContents, UUID libraryId) {
    Bundle bundle = queryBundle.copy();
    Library library = new Library()
        .setName(LIBRARY_NAME)
        .setVersion(LIBRARY_VERSION)
        .setStatus(ACTIVE)
        .setType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(CODE_SYSTEM_LIBRARY_TYPE)
                .setCode(CODE_SYSTEM_LIBRARY_TYPE_VALUE_LOGIC_LIBRARY)))
        .setUrl(createCanonicalUUIDUrn(libraryId));

    library.getMeta()
        .addTag(new Coding()
            .setSystem(CODE_SYSTEM_READ_ACCESS_TAG)
            .setCode(CODE_SYSTEM_READ_ACCESS_TAG_VALUE_ALL))
        .addProfile(LIBRARY_PROFILE);

    var attachments = queryContents.entrySet()
        .stream()
        .map(qc -> new Attachment().setContentType(qc.getKey()).setData(
            qc.getValue().getBytes(StandardCharsets.UTF_8)))
        .collect(Collectors.toList());

    library.setContent(attachments);

    bundle.addEntry()
        .setRequest(new BundleEntryRequestComponent()
            .setMethod(POST)
            .setUrl(REQUEST_URL_LIBRARY))
        .setResource(library)
        .setFullUrl(createCanonicalUUIDUrn(UUID.randomUUID()));

    return bundle;
  }

  /**
   * Given a query bundle adds a measure resource to it.
   * <p>
   * Adds a measure resource that references a library using the given library Id.
   *
   * @param queryBundle The measure is added to this query bundle.
   * @param libraryId   Identifies a library that this measure is going to use.
   * @return A copy of the given {@link Bundle} with the measure added.
   */
  private Bundle addMeasure(Bundle queryBundle, UUID libraryId) {
    Bundle bundle = queryBundle.copy();

    var measureId = UUID.randomUUID();
    var measureUrl = URI.create(fhirWebserviceClient.getBaseUrl()).resolve("./Measure/" + measureId);

    Task task = (Task) bundle.getEntryFirstRep().getResource();
    task.addInput()
        .setType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(CODE_SYSTEM_FEASIBILITY)
                .setCode(CODE_SYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE)))
        .setValue(new Reference()
            .setReference(createCanonicalUUIDUrn(measureId)));

    Measure measure = new Measure()
        .setUrl(measureUrl.toString())
        .setStatus(ACTIVE)
        .setScoring(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(CODE_SYSTEM_MEASURE_SCORING)
                .setCode(CODE_SYSTEM_MEASURE_SCORING_VALUE_COHORT)))
        .addLibrary(createCanonicalUUIDUrn(libraryId));

    var population = measure.addGroup().addPopulation();
    population.setId(UUID.randomUUID().toString());
    population.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(CODE_SYSTEM_MEASURE_POPULATION)
                .setCode(CODE_SYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION)))
        .setCriteria(new Expression()
            .setLanguage(MEASURE_CRITERIA_LANGUAGE)
            .setExpression(MEASURE_CRITERIA_EXPRESSION));

    measure.getMeta()
        .addTag(new Coding()
            .setSystem(CODE_SYSTEM_READ_ACCESS_TAG)
            .setCode(CODE_SYSTEM_READ_ACCESS_TAG_VALUE_ALL))
        .addProfile(MEASURE_PROFILE);

    bundle.addEntry()
        .setRequest(new BundleEntryRequestComponent()
            .setMethod(POST)
            .setUrl(REQUEST_URL_MEASURE))
        .setResource(measure)
        .setFullUrl(createCanonicalUUIDUrn(measureId));
    return bundle;
  }
}
