package de.medizininformatikinitiative.dataportal.backend.terminology.es;

import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.FeatureEsRepository;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.FeatureNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.elasticsearch.test.autoconfigure.DataElasticsearchTest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("terminology")
@Tag("elasticsearch")
@Import({FeatureService.class})
@Testcontainers
@DataElasticsearchTest
public class FeatureServiceIT {

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
  @Autowired
  private ElasticsearchOperations operations;
  @Autowired
  private FeatureEsRepository repo;
  @Autowired
  private FeatureService featureService;

  @BeforeAll
  static void setUp() throws InterruptedException {
    ELASTICSEARCH_CONTAINER.start();
    WebClient webClient = WebClient.builder().baseUrl("http://" + ELASTICSEARCH_CONTAINER.getHttpHostAddress()).build();
    webClient.put()
        .uri("/feature")
        .body(BodyInserters.fromResource(new ClassPathResource("feature_mapping.json", FeatureServiceIT.class)))
        .retrieve()
        .toBodilessEntity()
        .block();

    webClient.post()
        .uri("/feature/_bulk")
        .body(BodyInserters.fromResource(new ClassPathResource("feature_testdata.json", FeatureServiceIT.class)))
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
  void testPerformFeatureSearchWithRepoAndPaging_findsAllWithNoKeyword() {
    var page = assertDoesNotThrow(() -> featureService.performFeatureSearchWithRepoAndPaging("", null, null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(3L);
  }

  @Test
  void testPerformFeatureSearchWithRepoAndPaging_findsNone() {
    var page = assertDoesNotThrow(() -> featureService.performFeatureSearchWithRepoAndPaging("something-not-found", null, null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isZero();
    assertThat(page.getResults()).isEmpty();
  }

  @Test
  void testPerformFeatureSearchWithRepoAndPaging_findsByOriginalDisplay() {
    var page = assertDoesNotThrow(() -> featureService.performFeatureSearchWithRepoAndPaging("Prozedur", null, null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(1L);
    assertThat(page.getResults().get(0).id()).isEqualTo("module-prozedur-id");
  }

  @Test
  void testPerformFeatureSearchWithRepoAndPaging_findsByTranslatedField() {
    var page = assertDoesNotThrow(() -> featureService.performFeatureSearchWithRepoAndPaging("Feststellungsdatum", null, null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(1L);
    assertThat(page.getResults().get(0).id()).isEqualTo("diagnose-condition-id");
  }

  @Test
  void testPerformFeatureSearchWithRepoAndPaging_filtersByModule() {
    var page = assertDoesNotThrow(() -> featureService.performFeatureSearchWithRepoAndPaging("", List.of("Diagnose"), null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(2L);
    assertThat(page.getResults()).extracting("id").containsExactlyInAnyOrder("module-diagnose-id", "diagnose-condition-id");
  }

  @Test
  void testPerformFeatureSearchWithRepoAndPaging_filtersByCategories() {
    var page = assertDoesNotThrow(() -> featureService.performFeatureSearchWithRepoAndPaging("", null, List.of("element"), 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(1L);
    assertThat(page.getResults().get(0).id()).isEqualTo("diagnose-condition-id");
  }

  @Test
  void testGetFeatureListDetailsById_succeedsWithChildrenOnly() {
    var result = assertDoesNotThrow(() -> featureService.getFeatureListDetailsById("module-diagnose-id"));

    assertNotNull(result);
    assertThat(result.id()).isEqualTo("module-diagnose-id");
    assertThat(result.description()).isNull();
    assertThat(result.parents()).isEmpty();
    assertThat(result.children()).hasSize(1);
    assertThat(result.children().get(0).id()).isEqualTo("diagnose-condition-id");
  }

  @Test
  void testGetFeatureListDetailsById_succeedsWithParentsOnly() {
    var result = assertDoesNotThrow(() -> featureService.getFeatureListDetailsById("diagnose-condition-id"));

    assertNotNull(result);
    assertThat(result.id()).isEqualTo("diagnose-condition-id");
    assertThat(result.description()).isNotNull();
    assertThat(result.description().original()).isEqualTo("Diagnose Beschreibung");
    assertThat(result.parents()).hasSize(1);
    assertThat(result.parents().get(0).id()).isEqualTo("module-diagnose-id");
    assertThat(result.children()).isEmpty();
  }

  @Test
  void testGetFeatureListDetailsById_succeedsWithoutAnyRelations() {
    var result = assertDoesNotThrow(() -> featureService.getFeatureListDetailsById("module-prozedur-id"));

    assertNotNull(result);
    assertThat(result.parents()).isEmpty();
    assertThat(result.children()).isEmpty();
  }

  @Test
  void testGetFeatureListDetailsById_throwsOnNotFound() {
    assertThrows(FeatureNotFoundException.class, () -> featureService.getFeatureListDetailsById("does-not-exist"));
  }
}
