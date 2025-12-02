package de.fdpg.dataportal_backend.query.broker.direct;

import de.fdpg.dataportal_backend.query.broker.BrokerClient;
import de.fdpg.dataportal_backend.query.broker.QueryDefinitionNotFoundException;
import de.fdpg.dataportal_backend.query.collect.QueryStatus;
import de.fdpg.dataportal_backend.query.broker.QueryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;

import java.io.IOException;
import java.util.*;

import static de.fdpg.dataportal_backend.query.QueryMediaType.CQL;
import static de.fdpg.dataportal_backend.query.collect.QueryStatus.COMPLETED;
import static de.fdpg.dataportal_backend.query.collect.QueryStatus.FAILED;

/**
 * A {@link BrokerClient} to be used to directly communicate with a CQL-capable FHIR Server instance
 * without the need for using any middleware (Aktin or DSF).
 */
@Slf4j
public class DirectBrokerClientCql extends DirectBrokerClient {
    private final FhirConnector fhirConnector;
    private final FhirHelper fhirHelper;

    /**
     * Creates a new {@link DirectBrokerClientCql} instance that uses the given web client to
     * communicate with a CQL capable FHIR server instance.
     *
     * @param fhirConnector A FHIR connector.
     * @param fhirHelper
     */
    public DirectBrokerClientCql(FhirConnector fhirConnector,
        boolean obfuscateResultCount,
        FhirHelper fhirHelper) {
        super(obfuscateResultCount);
        this.fhirConnector = Objects.requireNonNull(fhirConnector);
        this.fhirHelper = fhirHelper;
        listeners = new ArrayList<>();
        brokerQueries = new HashMap<>();
    }

    @Override
    public void publishQuery(String brokerQueryId)
        throws QueryNotFoundException, IOException, QueryDefinitionNotFoundException {
        var query = findQuery(brokerQueryId);
        var queryContent = query.getQueryDefinition(CQL);

        updateQueryStatus(query, QueryStatus.EXECUTING);
        var libraryUri = "urn:uuid" + UUID.randomUUID();
        var measureUri = "urn:uuid" + UUID.randomUUID();
        MeasureReport measureReport;
        try {
            Bundle bundle = fhirHelper.createBundle(queryContent, libraryUri, measureUri);
            fhirConnector.transmitBundle(bundle);
            measureReport = fhirConnector.evaluateMeasure(measureUri);
        } catch (IOException e) {
            updateQueryStatus(query, FAILED);
            throw e;
        }

        var resultCount = measureReport.getGroupFirstRep().getPopulationFirstRep().getCount();
        query.setResult(obfuscateResultCount ? obfuscate(resultCount) : resultCount);
        updateQueryStatus(query, COMPLETED);
    }
}
