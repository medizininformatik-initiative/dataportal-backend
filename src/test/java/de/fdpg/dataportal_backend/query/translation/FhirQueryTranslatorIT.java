package de.fdpg.dataportal_backend.query.translation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fdpg.dataportal_backend.common.api.Criterion;
import de.fdpg.dataportal_backend.common.api.TermCode;
import de.fdpg.dataportal_backend.common.api.Unit;
import de.fdpg.dataportal_backend.query.api.AttributeFilter;
import de.fdpg.dataportal_backend.query.api.StructuredQuery;
import de.fdpg.dataportal_backend.query.api.TimeRestriction;
import de.fdpg.dataportal_backend.query.api.ValueFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.List;

import static de.fdpg.dataportal_backend.common.api.Comparator.GREATER_EQUAL;
import static de.fdpg.dataportal_backend.common.api.Comparator.GREATER_THAN;
import static de.fdpg.dataportal_backend.query.api.ValueFilterType.CONCEPT;
import static de.fdpg.dataportal_backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.testcontainers.containers.BindMode.READ_ONLY;

@Tag("query")
@Tag("translation")
@Disabled("Until we have a version of Flare that actually supports translating a structured query v2.")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = QueryTranslatorSpringConfig.class,
    properties = {
        "app.cqlTranslationEnabled=true",
        "app.fhirTranslationEnabled=false",
        "app.mappingsFile=./ontology/dataportal-term-code-mapping.json",
        "app.conceptTreeFile=./ontology/dataportal-code-tree.json"
    }
)
@Testcontainers
@SuppressWarnings("NewClassNamingConvention")
public class FhirQueryTranslatorIT {

  // Note that Flare does not come with a health endpoint. Thus, we will simply check if we get a response of any
  // kind in order to assume that the server is up rendering the container ready.
  @Container
  private final GenericContainer<?> flare = new GenericContainer<>(DockerImageName.parse("ghcr.io/medizininformatik-initiative/flare:2.5.0"))
      .withExposedPorts(5000)
      .withFileSystemBind("ontology/dataportal-code-tree.json", "/opt/flare/src/query_parser/codex/codex-code-tree.json", READ_ONLY)
      .withFileSystemBind("ontology/dataportal-term-code-mapping.json", "/opt/flare/src/query_parser/codex/codex-mapping.json", READ_ONLY)
      .waitingFor(Wait.forHttp("/")
          .forStatusCodeMatching(c -> c >= 200 && c <= 500))
      .withStartupAttempts(5);
  @Autowired
  @Qualifier("translation")
  private ObjectMapper jsonUtil;
  private FhirQueryTranslator fhirQueryTranslator;

  @BeforeEach
  public void setUp() {
    var flareRootUri = UriComponentsBuilder.newInstance()
        .scheme("http")
        .host(flare.getHost())
        .port(flare.getFirstMappedPort())
        .build()
        .toString();

    var client = new RestTemplateBuilder()
        .rootUri(flareRootUri)
        .build();

    fhirQueryTranslator = new FhirQueryTranslator(client, jsonUtil);
  }

  @Test
  public void testTranslate() {
    var bodyWeightTermCode = TermCode.builder()
        .code("27113001")
        .system("http://snomed.info/sct")
        .version("v1")
        .display("Body weight (observable entity)")
        .build();
    var kgUnit = Unit.builder()
        .code("kg")
        .display("kilogram")
        .build();
    var bodyWeightValueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_EQUAL)
        .quantityUnit(kgUnit)
        .value(50.0)
        .build();
    var hasBmiGreaterThanFifty = Criterion.builder()
        .termCodes(List.of(bodyWeightTermCode))
        .valueFilter(bodyWeightValueFilter)
        .build();
    var testQuery = StructuredQuery.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
        .build();

    @SuppressWarnings("unused")
    var translationResult = assertDoesNotThrow(() -> fhirQueryTranslator.translate(testQuery));
    // TODO: add assertions!
  }

  @Test
  public void testTranslate_SupportsTimeRestrictions() {
    var dementiaTermCode = TermCode.builder()
        .code("F00")
        .system("http://fhir.de/CodeSystem/dimdi/icd-10-gm")
        .display("F00")
        .build();
    var hasDementia = Criterion.builder()
        .termCodes(List.of(dementiaTermCode))
        .build();
    var psychologicalDysfunctionTermCode = TermCode.builder()
        .code("F00")
        .system("http://fhir.de/CodeSystem/dimdi/icd-10-gm")
        .display("F00")
        .build();
    var timeRestriction = new TimeRestriction("2021-10-09", "2021-09-09");
    var hasPsychologicalDysfunction = Criterion.builder()
        .termCodes(List.of(psychologicalDysfunctionTermCode))
        .timeRestriction(timeRestriction)
        .build();
    var testQuery = StructuredQuery.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(hasDementia, hasPsychologicalDysfunction)))
        .build();

    @SuppressWarnings("unused")
    var translationResult = assertDoesNotThrow(() -> fhirQueryTranslator.translate(testQuery));
    // TODO: add assertions!
  }

  @Test
  public void testTranslate_SupportsAttributeFilters() {
    var ageTermCode = TermCode.builder()
        .code("30525-0")
        .system("http://loinc.org")
        .display("Alter")
        .build();
    var yearUnit = Unit.builder()
        .code("a")
        .display("Jahr")
        .build();
    var ageValueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_THAN)
        .quantityUnit(yearUnit)
        .value(18.0)
        .build();
    var olderThanEighteen = Criterion.builder()
        .termCodes(List.of(ageTermCode))
        .valueFilter(ageValueFilter)
        .build();
    var bodyTemperatureTermCode = TermCode.builder()
        .code("8310-5")
        .system("http://loinc.org")
        .display("Körpertemperatur")
        .build();
    var axillaryMeasureMethod = TermCode.builder()
        .code("LA9370-3")
        .system("http://loinc.org")
        .display("Axillary")
        .build();
    var method = TermCode.builder()
        .code("method")
        .system("abide")
        .display("method")
        .build();
    var axillaryMeasured = AttributeFilter.builder()
        .type(CONCEPT)
        .selectedConcepts(List.of(axillaryMeasureMethod))
        .attributeCode(method)
        .build();
    var bodyTemperatureAxillaryMeasured = Criterion.builder()
        .termCodes(List.of(bodyTemperatureTermCode))
        .attributeFilters(List.of(axillaryMeasured))
        .build();
    var testQuery = StructuredQuery.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(olderThanEighteen)))
        .exclusionCriteria(List.of(List.of(bodyTemperatureAxillaryMeasured)))
        .build();

    @SuppressWarnings("unused")
    var translationResult = assertDoesNotThrow(() -> fhirQueryTranslator.translate(testQuery));
    // TODO: add assertions!
  }
}
