package de.fdpg.dataportal_backend.query.broker.dsf;

import dev.dsf.fhir.client.FhirWebserviceClient;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;

import java.util.Optional;

/**
 * A handler that is capable of processing a FHIR Task which represents the result of a feasibility query that ran
 * in a single DIC.
 */
@Slf4j
class DSFQueryResultHandler {

    private static final String SINGLE_DIC_QUERY_RESULT_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/feasibility-task-single-dic-result|1.0";
    private static final String CODE_SYSTEM_FEASIBILITY = "http://medizininformatik-initiative.de/fhir/CodeSystem/feasibility";
    private static final String CODE_SYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REF = "measure-report-reference";
    private static final String CODE_SYSTEM_BPMN_MESSAGE = "http://dsf.dev/fhir/CodeSystem/bpmn-message";
    private static final String CODE_SYSTEM_BPMN_MESSAGE_VALUE_BUSINESS_KEY = "business-key";

    private static final String CODE_SYSTEM_MEASURE_POPULATION = "http://terminology.hl7.org/CodeSystem/measure-population";
    private static final String CODE_SYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION = "initial-population";

    private final FhirWebClientProvider fhirWebClientProvider;
    private FhirWebserviceClient fhirWebserviceClient;

    /**
     * Creates a new {@link DSFQueryResultHandler} instance for processing FHIR Task resources.
     *
     * @param fhirWebClientProvider Provider capable of providing a FHIR webservice client for communicating with a
     *                              FHIR server via HTTP.
     */
    public DSFQueryResultHandler(FhirWebClientProvider fhirWebClientProvider) {
        this.fhirWebClientProvider = fhirWebClientProvider;
    }

    /**
     * Given a {@link DomainResource} creates a {@link DSFQueryResult}.
     * <p>
     * Only {@link DomainResource}s that resemble a {@link Task} will be taken into account. Such a task itself has to
     * have a profile indicating that it carries information regarding a feasibility query result of a single DIC.
     *
     * @param resource An arbitrary FHIR resource.
     * @return The feasibility query result if there is any.
     */
    public Optional<DSFQueryResult> onResult(Resource resource) {
        if (resource instanceof Task task) {

            if (!hasSingleQueryResultProfile(task)) {
                log.info("Ignoring task without single DIC query result profile.");
                return Optional.empty();
            }

            String queryId = extractQueryId(task);
            String siteId = task.getRequester().getIdentifier().getValue();
            IdType measureReportUrl = extractMeasureReportId(task);

          log.info("Received query result of query with ID '{}' for site with ID '{}'", queryId, siteId);

            try {
                MeasureReport report = fetchMeasureReport(measureReportUrl.getIdPart());
                deleteMeasureReport(measureReportUrl.getIdPart());
                int measureCount = extractMeasureCount(report);
                return Optional.of(new DSFQueryResult(queryId, siteId, measureCount));
            } catch (Exception e) {
              log.error("Could not fetch measure report with ID '{}' to get the measure count of query with ID '{}' for site with ID '{}': {}", measureReportUrl.getIdPart(), queryId, siteId, e.getMessage(), e);
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Checks whether the given task has a single query result profile attached to it.
     *
     * @param task The task that shall be checked for the profile.
     * @return True if the task has a single query result profile attached to it and false otherwise.
     */
    private boolean hasSingleQueryResultProfile(Task task) {
        return task.getMeta().getProfile().stream().anyMatch(p -> p.getValue().equals(SINGLE_DIC_QUERY_RESULT_PROFILE));
    }

    /**
     * Given a FHIR Task extracts the query ID from it.
     *
     * @param task The task that the query ID is extracted from.
     * @return The query ID.
     */
    private String extractQueryId(Task task) {
        return task.getInput()
                .stream()
                .filter(i -> i.getType().getCodingFirstRep().getSystem().equals(CODE_SYSTEM_BPMN_MESSAGE)
                        && i.getType().getCodingFirstRep().getCode().equals(CODE_SYSTEM_BPMN_MESSAGE_VALUE_BUSINESS_KEY))
                .findFirst().orElseThrow().getValue().toString();
    }

    /**
     * Given a FHIR Task extracts the URL pointing to the measure report holding any measure count information.
     *
     * @param task The task that the measure report URL is extracted from.
     * @return The measure report URL.
     */
    private IdType extractMeasureReportId(Task task) {
        Reference measureReportReference = (Reference) task.getOutput()
                .stream()
                .filter(o -> o.getType().getCodingFirstRep().getSystem().equals(CODE_SYSTEM_FEASIBILITY)
                        && o.getType().getCodingFirstRep().getCode().equals(CODE_SYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REF))
                .findFirst().orElseThrow().getValue();

        return new IdType(measureReportReference.getReference());
    }

    /**
     * Fetches a specific measure report and returns it.
     *
     * @param measureReportId Identifies the measure report that shall be fetched.
     * @return The fetched measure report.
     */
    private MeasureReport fetchMeasureReport(String measureReportId) throws FhirWebClientProvisionException {
        if (fhirWebserviceClient == null) {
            fhirWebserviceClient = fhirWebClientProvider.provideFhirWebserviceClient();
        }
        return fhirWebserviceClient.read(MeasureReport.class, measureReportId);
    }

    /**
     * Deletes a specific measure report in DSF.
     *
     * @param measureReportId Identifies the measure report that shall be deleted.
     */
    private void deleteMeasureReport(String measureReportId) throws FhirWebClientProvisionException {
        if (fhirWebserviceClient == null) {
            fhirWebserviceClient = fhirWebClientProvider.provideFhirWebserviceClient();
        }
        fhirWebserviceClient.delete(MeasureReport.class, measureReportId);
        fhirWebserviceClient.deletePermanently(MeasureReport.class, measureReportId);
    }

    /**
     * Given a {@link MeasureReport} extracts the measure count from it.
     *
     * @param report The report from which the measure count shall be extracted.
     * @return The extracted measure count.
     */
    private int extractMeasureCount(MeasureReport report) {
        return report.getGroup()
                .stream()
                .filter(g -> g.getPopulationFirstRep().getCode().getCodingFirstRep().getSystem().equals(CODE_SYSTEM_MEASURE_POPULATION)
                        && g.getPopulationFirstRep().getCode().getCodingFirstRep().getCode().equals(CODE_SYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION))
                .findFirst().orElseThrow().getPopulationFirstRep().getCount();
    }
}
