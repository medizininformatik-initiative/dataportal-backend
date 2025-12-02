package de.fdpg.dataportal_backend.query.broker.dsf;

import dev.dsf.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskRestrictionComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hl7.fhir.r4.model.Task.TaskIntent.ORDER;
import static org.hl7.fhir.r4.model.Task.TaskStatus.COMPLETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DSFQueryResultHandlerTest {

    @Mock
    FhirWebserviceClient client;

    @Mock
    FhirWebClientProvider fhirWebClientProvider;

    @InjectMocks
    DSFQueryResultHandler handler;

    @Test
    public void testOnResultButResourceIsNoTask() {
        Patient patient = new Patient();
        Optional<DSFQueryResult> queryResult = handler.onResult(patient);

        assertFalse(queryResult.isPresent());
    }

    @Test
    public void testOnResultButResourceHasIncorrectProfile() {
        Task task = new Task();
        task.getMeta().addProfile("incorrect-profile");
        Optional<DSFQueryResult> dsfQueryResult = handler.onResult(task);

        assertFalse(dsfQueryResult.isPresent());
    }

    @Test
    public void testOnResultButReferencedMeasureReportCanNotBeFetched() throws FhirWebClientProvisionException {
        Reference dicOrganizationRef = new Reference().setIdentifier(
                new Identifier().setSystem("http://dsf.dev/fhir/NamingSystem/organization-identifier").setValue("DIC"));
        Reference zarsOrganizationRef = new Reference().setIdentifier(new Identifier()
                .setSystem("http://dsf.dev/fhir/NamingSystem/organization-identifier").setValue("ZARS"));

        Task task = new Task()
                .setStatus(COMPLETED)
                .setIntent(ORDER)
                .setRequester(dicOrganizationRef)
                .setRestriction(new TaskRestrictionComponent().addRecipient(zarsOrganizationRef));
        task.getMeta().addProfile(
                "http://medizininformatik-initiative.de/fhir/StructureDefinition/feasibility-task-single-dic-result|1.0");

        task.addInput()
                .setType(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
                                .setCode("business-key")))
                .setValue(new StringType("1234567890"));
        task.addOutput()
                .setType(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://medizininformatik-initiative.de/fhir/CodeSystem/feasibility")
                                .setCode("measure-report-reference")))
                .setValue(new Reference().setReference("MeasureReport/dfd68241-224d-4fd8-bd1a-7675682fa608"));

        when(fhirWebClientProvider.provideFhirWebserviceClient()).thenReturn(client);
        Exception e = new RuntimeException("cannot fetch measure report");
        when(client.read(MeasureReport.class, "dfd68241-224d-4fd8-bd1a-7675682fa608")).thenThrow(e);

        Optional<DSFQueryResult> dsfQueryResult = handler.onResult(task);
        assertFalse(dsfQueryResult.isPresent());
    }

    @Test
    public void testOnResult() throws FhirWebClientProvisionException {
        Reference dicOrganizationRef = new Reference().setIdentifier(
                new Identifier().setSystem("http://dsf.dev/fhir/NamingSystem/organization-identifier").setValue("DIC"));
        Reference zarsOrganizationRef = new Reference().setIdentifier(new Identifier()
                .setSystem("http://dsf.dev/fhir/NamingSystem/organization-identifier").setValue("ZARS"));

        Task task = new Task()
                .setStatus(COMPLETED)
                .setIntent(ORDER)
                .setRequester(dicOrganizationRef)
                .setRestriction(new TaskRestrictionComponent().addRecipient(zarsOrganizationRef));
        task.getMeta().addProfile(
                "http://medizininformatik-initiative.de/fhir/StructureDefinition/feasibility-task-single-dic-result|1.0");

        task.addInput()
                .setType(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
                                .setCode("business-key")))
                .setValue(new StringType("1234567890"));
        task.addOutput()
                .setType(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://medizininformatik-initiative.de/fhir/CodeSystem/feasibility")
                                .setCode("measure-report-reference")))
                .setValue(new Reference().setReference("MeasureReport/dfd68241-224d-4fd8-bd1a-7675682fa608"));

        MeasureReport report = new MeasureReport();
        report.addGroup()
                .setCode(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://medizininformatik-initiative.de/fhir/CodeSystem/feasibility")
                                .setCode("single")))
                .addPopulation()
                .setCode(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/measure-population")
                                .setCode("initial-population")))
                .setCount(10);

        when(fhirWebClientProvider.provideFhirWebserviceClient()).thenReturn(client);
        when(client.read(MeasureReport.class, "dfd68241-224d-4fd8-bd1a-7675682fa608")).thenReturn(report);

        Optional<DSFQueryResult> dsfQueryResult = handler.onResult(task);
        assertTrue(dsfQueryResult.isPresent());
        assertEquals("1234567890", dsfQueryResult.get().getQueryId());
        assertEquals("DIC", dsfQueryResult.get().getSiteId());
        assertEquals(10, dsfQueryResult.get().getMeasureCount());
    }
}
