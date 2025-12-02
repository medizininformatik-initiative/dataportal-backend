package de.fdpg.dataportal_backend.terminology.es;

import de.fdpg.dataportal_backend.common.api.TermCode;
import de.fdpg.dataportal_backend.terminology.api.CodeableConceptBulkSearchRequest;
import de.fdpg.dataportal_backend.terminology.api.CodeableConceptEntry;
import de.fdpg.dataportal_backend.terminology.es.repository.CodeableConceptEsRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("terminology")
@Tag("elasticsearch")
@Import({CodeableConceptService.class})
@Testcontainers
@DataElasticsearchTest(        properties = {
    "app.elastic.filter=context,terminology"
})
public class CodeableConceptServiceIT {

  @Autowired
  private ElasticsearchOperations operations;

  @Autowired
  private CodeableConceptEsRepository repo;

  @Autowired
  private CodeableConceptService codeableConceptService;

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
  static void setUp() throws InterruptedException {
    ELASTICSEARCH_CONTAINER.start();
    WebClient webClient = WebClient.builder().baseUrl("http://" + ELASTICSEARCH_CONTAINER.getHttpHostAddress()).build();
    webClient.put()
        .uri("/codeable_concept")
        .body(BodyInserters.fromResource(new ClassPathResource("codeable_concept.json", CodeableConceptServiceIT.class)))
        .retrieve()
        .toBodilessEntity()
        .block();

    webClient.post()
        .uri("/codeable_concept/_bulk")
        .body(BodyInserters.fromResource(new ClassPathResource("cc_testdata.json", CodeableConceptServiceIT.class)))
        .retrieve()
        .toBodilessEntity()
        .block();

    // When running in github actions without a slight delay, the data might not be complete in the elastic search container (although a blocking call is used)
    Thread.sleep(1000);
  }

  @AfterAll
  static void tearDown() {
    ELASTICSEARCH_CONTAINER.stop();
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_findsOne() {
    var page = assertDoesNotThrow (() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", List.of(), 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isOne();
    Assertions.assertEquals("A1.0", page.getResults().get(0).termCode().code());
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_findsNone() {
    var page = assertDoesNotThrow (() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("something-not-found", List.of(), 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isZero();
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_findsAllWithNoKeyword() {
    var page = assertDoesNotThrow (() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("", List.of(), 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(3L);
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_findsTwoOrOneDependingOnFilter() {
    var pageNoFilter = assertDoesNotThrow (() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("ba", List.of(), 20, 0));
    var pageOneFilter = assertDoesNotThrow (() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("ba", List.of("some-value-set"), 20, 0));

    assertNotNull(pageNoFilter);
    assertNotNull(pageOneFilter);
    Assertions.assertEquals(2, pageNoFilter.getTotalHits());
    Assertions.assertEquals(1, pageOneFilter.getTotalHits());
  }

  @Test
  void testPerformExactSearch_succeeds() {
    List<String> expectedFoundResults = List.of("A1.1");
    List<String> expectedNotFoundResults = List.of();
    List<String> searchterms =
        Stream.concat(expectedFoundResults.stream(), expectedNotFoundResults.stream()).collect(Collectors.toList());
    var request = CodeableConceptBulkSearchRequest.builder()
        .searchterms(searchterms)
        .valueSet("some-value-set")
        .build();

    var exactResult =  assertDoesNotThrow(
        () -> codeableConceptService.performExactSearch(request)
    );

    assertThat(exactResult).isNotNull();
    assertThat(exactResult.notFound().size()).isEqualTo(expectedNotFoundResults.size());
    assertThat(exactResult.found().size()).isEqualTo(expectedFoundResults.size());
    assertThat(exactResult.found().get(0)).isInstanceOf(CodeableConceptEntry.class);
    assertThat(exactResult.found().get(0).termCode().code()).isEqualTo("A1.1");
    assertThat(exactResult.notFound()).containsAll(expectedNotFoundResults);
  }

  @Test
  void testPerformExactSearch_succeedsWithMixedResults() {
    List<String> expectedFoundResults = List.of("A1.1");
    List<String> expectedNotFoundResults = List.of("foo");
    List<String> searchterms =
        Stream.concat(expectedFoundResults.stream(), expectedNotFoundResults.stream()).collect(Collectors.toList());
    var request = CodeableConceptBulkSearchRequest.builder()
        .searchterms(searchterms)
        .valueSet("some-value-set")
        .build();

    var exactResult =  assertDoesNotThrow(
        () -> codeableConceptService.performExactSearch(request)
    );

    assertThat(exactResult).isNotNull();
    assertThat(exactResult.notFound().size()).isEqualTo(expectedNotFoundResults.size());
    assertThat(exactResult.found().size()).isEqualTo(expectedFoundResults.size());
    assertThat(exactResult.found().get(0)).isInstanceOf(CodeableConceptEntry.class);
    assertThat(exactResult.found().get(0).termCode().code()).isEqualTo("A1.1");
    assertThat(exactResult.notFound()).containsAll(expectedNotFoundResults);
  }

  @Test
  void testPerformExactSearch_succeedsWithInvalidValueSet() {
    List<String> expectedFoundResults = List.of();
    List<String> expectedNotFoundResults = List.of("A1.1");
    List<String> searchterms =
        Stream.concat(expectedFoundResults.stream(), expectedNotFoundResults.stream()).collect(Collectors.toList());
    var request = CodeableConceptBulkSearchRequest.builder()
        .searchterms(searchterms)
        .valueSet("does not exist")
        .build();

    var exactResult =  assertDoesNotThrow(
        () -> codeableConceptService.performExactSearch(request)
    );

    assertThat(exactResult).isNotNull();
    assertThat(exactResult.notFound().size()).isEqualTo(expectedNotFoundResults.size());
    assertThat(exactResult.found().size()).isEqualTo(expectedFoundResults.size());
    assertThat(exactResult.notFound()).containsAll(expectedNotFoundResults);
  }

  @Test
  void testPerformExactSearch_succeedsWithEmptySearchterms() {
    List<String> searchterms = List.of();
    var request = CodeableConceptBulkSearchRequest.builder()
        .searchterms(searchterms)
        .valueSet("some-value-set")
        .build();

    var exactResult =  assertDoesNotThrow(
        () -> codeableConceptService.performExactSearch(request)
    );

    assertThat(exactResult).isNotNull();
    assertThat(exactResult.notFound().isEmpty());
    assertThat(exactResult.found().isEmpty());
  }

  @Test
  void testGetSearchResultsEntryByIds_succeeds() {
    var result = assertDoesNotThrow(() -> codeableConceptService.getSearchResultsEntryByIds(List.of("ff98eecf-9e2c-35e7-b39b-45d4e29aee2e")));

    assertNotNull(result);
    Assertions.assertFalse(result.isEmpty());
    Assertions.assertEquals("bar", result.get(0).termCode().display());
    Assertions.assertEquals("A1.1", result.get(0).termCode().code());
    Assertions.assertEquals("2012", result.get(0).termCode().version());
    Assertions.assertEquals("another-system", result.get(0).termCode().system());
  }

  @Test
  void testGetSearchResultsEntryByIds_emptyOnNotFound() {
    var result = assertDoesNotThrow(() -> codeableConceptService.getSearchResultsEntryByIds(List.of("something-not-found")));
    assertNotNull(result);
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void testGetSearchResultsEntryByTermcode_succeeds() {
    var tc = createTermCode();
    var result = assertDoesNotThrow(() -> codeableConceptService.getSearchResultEntryByTermCode(tc));

    assertNotNull(result);
    Assertions.assertNotNull(result.display());
    Assertions.assertEquals(tc.code(), result.termCode().code());
    Assertions.assertEquals(tc.system(), result.termCode().system());
    Assertions.assertEquals(tc.version(), result.termCode().version());
  }

  @Test
  void testGetSearchResultsEntryByTermcode_nullOnNotFound() {
    var result = assertDoesNotThrow(() -> codeableConceptService.getSearchResultEntryByTermCode(TermCode.builder().build()));
    Assertions.assertNull(result);
  }

  private TermCode createTermCode() {
    return TermCode.builder()
        .code("A2.0")
        .system("some-system")
        .version("2023")
        .build();
  }
}
