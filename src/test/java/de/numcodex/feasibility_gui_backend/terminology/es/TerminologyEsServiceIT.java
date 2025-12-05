package de.numcodex.feasibility_gui_backend.terminology.es;

import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntryExtended;
import de.numcodex.feasibility_gui_backend.terminology.api.RelativeEntry;
import de.numcodex.feasibility_gui_backend.terminology.api.TerminologyBulkSearchRequest;
import de.numcodex.feasibility_gui_backend.terminology.es.model.TermFilter;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemEsRepository;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemNotFoundException;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyListItemEsRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("terminology")
@Tag("elasticsearch")
@Import({TerminologyEsService.class})
@Testcontainers
@DataElasticsearchTest(        properties = {
    "app.elastic.filter=context,terminology"
})
public class TerminologyEsServiceIT {

  @Autowired
  private OntologyListItemEsRepository ontologyListItemEsRepository;

  @Autowired
  private OntologyItemEsRepository ontologyItemEsRepository;

  @Autowired
  private TerminologyEsService terminologyEsService;

  @Container
  @ServiceConnection
  public static ElasticsearchContainer ELASTICSEARCH_CONTAINER = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.1.4")
      .withEnv("discovery.type", "single-node")
      .withEnv("xpack.security.enabled", "false")
      .withReuse(false)
      .withExposedPorts(9200)
      .withStartupAttempts(3)
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .waitingFor(Wait.forHttp("/health").forStatusCodeMatching(c -> c >= 200 && c <= 500));

  @BeforeAll
  static void setUp() throws IOException, InterruptedException {
    ELASTICSEARCH_CONTAINER.start();
    System.out.println(ELASTICSEARCH_CONTAINER.getHttpHostAddress());
    WebClient webClient = WebClient.builder().baseUrl("http://" + ELASTICSEARCH_CONTAINER.getHttpHostAddress()).build();
    webClient.put()
        .uri("/ontology")
        .body(BodyInserters.fromResource(new ClassPathResource("ontology.json", TerminologyEsServiceIT.class)))
        .retrieve()
        .toBodilessEntity()
        .block();

    webClient.post()
        .uri("/ontology/_bulk")
        .body(BodyInserters.fromResource(new ClassPathResource("ontology_testdata.json", TerminologyEsServiceIT.class)))
        .retrieve()
        .toBodilessEntity()
        .block();

    // There may be a timing issue when there is no short sleep here, leading to tests failing because the data is not fully loaded
    // Although the commands to upload the data are blocking
    sleep(1000);
  }

  @AfterAll
  static void tearDown() {
    ELASTICSEARCH_CONTAINER.stop();
  }

  @Test
  void testGetAvailableFilters() {
    List<TermFilter> availableFilters = terminologyEsService.getAvailableFilters();
    assertThat(availableFilters).isNotNull();

    assertThat(availableFilters)
        .hasSize(3)
        .extracting(TermFilter::name, TermFilter::type)
        .containsExactlyInAnyOrder(
            tuple("availability", "boolean"),
            tuple("context", "selectable-concept"),
            tuple("terminology", "selectable-concept")
        );
  }

  @Test
  void testPerformOntologySearchWithPaging_zeroResults() {
    var page = terminologyEsService.performOntologySearchWithPaging("random searchterm that is not found", null,null, null, null, false, 20, 0);
    assertThat(page).isNotNull();
    assertThat(page.totalHits()).isZero();
  }

  @Test
  void testPerformOntologySearchWithPaging_findsAllWithNoKeyword() {
    var page = terminologyEsService.performOntologySearchWithPaging("", null, null, null, null, false, 20, 0);
    assertThat(page).isNotNull();
    assertThat(page.totalHits()).isEqualTo(4L);
  }

  @Test
  void testPerformOntologySearchWithPaging_oneResult() {
    var page = terminologyEsService.performOntologySearchWithPaging("Hauttransplan", null, null, null, null, false, 20, 0);
    assertThat(page).isNotNull();
    assertThat(page.totalHits()).isOne();
    assertThat(page.results().get(0).terminology()).isEqualTo("http://fhir.de/CodeSystem/bfarm/ops");
    assertThat(page.results().get(0).termcode()).isEqualTo("5-925.gb");
  }

  @Test
  void testPerformOntologySearchWithPaging_multipleResults() {
    var page = terminologyEsService.performOntologySearchWithPaging("Blutdr", null, null, null, null, false, 20, 0);
    assertThat(page).isNotNull();
    assertThat(page.totalHits()).isEqualTo(3);
    assertThat(page.results().size()).isEqualTo(3);
    assertThat(page.results().get(0).terminology()).isEqualTo("http://fhir.de/CodeSystem/bfarm/icd-10-gm");
    assertThat(page.results().get(0).termcode()).containsIgnoringCase("r03");
  }

  @Test
  void testPerformOntologySearchWithPaging_multipleResultsMultiplePagesPage0() {
    var page = terminologyEsService.performOntologySearchWithPaging("Blutdr", null, null, null, null, false, 2, 0);
    assertThat(page).isNotNull();
    assertThat(page.totalHits()).isEqualTo(3);
    assertThat(page.results().size()).isEqualTo(2);
    assertThat(page.results().get(0).terminology()).isEqualTo("http://fhir.de/CodeSystem/bfarm/icd-10-gm");
    assertThat(page.results().get(0).termcode()).containsIgnoringCase("r03");
  }

  @Test
  void testPerformOntologySearchWithPaging_multipleResultsMultiplePagesPage1() {
    var page = terminologyEsService.performOntologySearchWithPaging("Blutdr", null, null, null, null, false, 2, 1);
    assertThat(page).isNotNull();
    assertThat(page.totalHits()).isEqualTo(3);
    assertThat(page.results().size()).isEqualTo(1);
    assertThat(page.results().get(0).terminology()).isEqualTo("http://fhir.de/CodeSystem/bfarm/icd-10-gm");
    assertThat(page.results().get(0).termcode()).containsIgnoringCase("r03");
  }

  @Test
  void testPerformExactSearch_succeeds() {
    List<String> expectedFoundResults = List.of("R03", "R03.0", "R03.1");
    List<String> expectedNotFoundResults = List.of();
    List<String> searchterms =
        Stream.concat(expectedFoundResults.stream(), expectedNotFoundResults.stream()).collect(Collectors.toList());
    var request = TerminologyBulkSearchRequest.builder()
        .searchterms(searchterms)
        .context("Diagnose")
        .terminology("http://fhir.de/CodeSystem/bfarm/icd-10-gm")
        .build();

    var exactResult =  assertDoesNotThrow(
        () -> terminologyEsService.performExactSearch(request)
    );

    assertThat(exactResult).isNotNull();
    assertThat(exactResult.notFound().size()).isEqualTo(expectedNotFoundResults.size());
    assertThat(exactResult.found().size()).isEqualTo(expectedFoundResults.size());
    assertThat(exactResult.found().get(0)).isInstanceOf(EsSearchResultEntryExtended.class);
    assertThat(exactResult.found().get(0).terminology()).isEqualTo("http://fhir.de/CodeSystem/bfarm/icd-10-gm");
    assertThat(exactResult.found().get(0).context().code()).isEqualTo("Diagnose");
    assertThat(exactResult.found().get(0).termcodes().get(0).code()).startsWith("R03");
  }

  @Test
  void testPerformExactSearch_succeedsWithMixedResults() {
    List<String> expectedFoundResults = List.of("R03.1");
    List<String> expectedNotFoundResults = List.of("foo", "bar");
    List<String> searchterms =
        Stream.concat(expectedFoundResults.stream(), expectedNotFoundResults.stream()).collect(Collectors.toList());
    var request = TerminologyBulkSearchRequest.builder()
        .searchterms(searchterms)
        .context("Diagnose")
        .terminology("http://fhir.de/CodeSystem/bfarm/icd-10-gm")
        .build();

    var exactResult =  assertDoesNotThrow(
        () -> terminologyEsService.performExactSearch(request)
    );

    assertThat(exactResult).isNotNull();
    assertThat(exactResult.notFound().size()).isEqualTo(expectedNotFoundResults.size());
    assertThat(exactResult.found().size()).isEqualTo(expectedFoundResults.size());
    assertThat(exactResult.found().get(0)).isInstanceOf(EsSearchResultEntryExtended.class);
    assertThat(exactResult.found().get(0).terminology()).isEqualTo("http://fhir.de/CodeSystem/bfarm/icd-10-gm");
    assertThat(exactResult.found().get(0).context().code()).isEqualTo("Diagnose");
    assertThat(exactResult.found().get(0).termcodes().get(0).code()).startsWith("R03");
    assertThat(exactResult.notFound()).containsAll(expectedNotFoundResults);
  }

  @Test
  void testPerformExactSearch_succeedsWithInvalidContext() {
    List<String> expectedFoundResults = List.of();
    List<String> expectedNotFoundResults = List.of("R03.1", "foo", "bar");
    List<String> searchterms =
        Stream.concat(expectedFoundResults.stream(), expectedNotFoundResults.stream()).collect(Collectors.toList());
    var request = TerminologyBulkSearchRequest.builder()
        .searchterms(searchterms)
        .context("some invalid context")
        .terminology("http://fhir.de/CodeSystem/bfarm/icd-10-gm")
        .build();

    var exactResult =  assertDoesNotThrow(
        () -> terminologyEsService.performExactSearch(request)
    );

    assertThat(exactResult).isNotNull();
    assertThat(exactResult.notFound().size()).isEqualTo(expectedNotFoundResults.size());
    assertThat(exactResult.found().size()).isEqualTo(expectedFoundResults.size());
    assertThat(exactResult.notFound()).containsAll(expectedNotFoundResults);
  }

  @Test
  void testPerformExactSearch_succeedsWithInvalidTerminology() {
    List<String> expectedFoundResults = List.of();
    List<String> expectedNotFoundResults = List.of("R03.1", "foo", "bar");
    List<String> searchterms =
        Stream.concat(expectedFoundResults.stream(), expectedNotFoundResults.stream()).collect(Collectors.toList());
    var request = TerminologyBulkSearchRequest.builder()
        .searchterms(searchterms)
        .context("Diagnose")
        .terminology("some invalid terminology")
        .build();

    var exactResult =  assertDoesNotThrow(
        () -> terminologyEsService.performExactSearch(request)
    );

    assertThat(exactResult).isNotNull();
    assertThat(exactResult.notFound().size()).isEqualTo(expectedNotFoundResults.size());
    assertThat(exactResult.found().size()).isEqualTo(expectedFoundResults.size());
    assertThat(exactResult.notFound()).containsAll(expectedNotFoundResults);
  }

  @Test
  void testPerformExactSearch_succeedsWithEmptySearchterms() {
    List<String> searchterms = List.of();
    var request = TerminologyBulkSearchRequest.builder()
        .searchterms(searchterms)
        .context("Diagnose")
        .terminology("http://fhir.de/CodeSystem/bfarm/icd-10-gm")
        .build();

    var exactResult =  assertDoesNotThrow(
        () -> terminologyEsService.performExactSearch(request)
    );

    assertThat(exactResult).isNotNull();
    assertThat(exactResult.notFound().isEmpty());
    assertThat(exactResult.found().isEmpty());
  }

  @Test
  void testGetSearchResultEntryByHash_succeeds() {
    String entryId = "1026c3ef-9f0a-3db3-94e7-7615c8041706";
    EsSearchResultEntry entry = assertDoesNotThrow(() -> terminologyEsService.getSearchResultEntryByHash(entryId));
    assertThat(entry).isNotNull();
    assertThat(entry.id()).isEqualTo(entryId);
  }

  @Test
  void testGetSearchResultEntryByHash_throwsOnNotFound() {
    assertThrows(OntologyItemNotFoundException.class, () -> terminologyEsService.getSearchResultEntryByHash("invalid-id"));
  }

  @Test
  void testGetSearchRelationsByHash_succeeds() {
    String entryId = "1026c3ef-9f0a-3db3-94e7-7615c8041706";
    var relations = assertDoesNotThrow(() -> terminologyEsService.getRelationEntryByHash(entryId));
    assertThat(relations).isNotNull();
    assertThat(relations.parents()).isNotNull();
    assertThat(relations.parents()).isNotEmpty();
    assertThat(relations.parents().stream().toList().get(0)).isInstanceOf(RelativeEntry.class);
  }

  @Test
  void testGetSearchRelationsByHash_throwsOnNotFound() {
    assertThrows(OntologyItemNotFoundException.class, () -> terminologyEsService.getRelationEntryByHash("invalid-id"));
  }
}
