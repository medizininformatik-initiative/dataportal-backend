package de.medizininformatikinitiative.dataportal.backend.terminology.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import de.medizininformatikinitiative.dataportal.backend.config.PropertiesReader;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.OntologyVersionCheck.Mode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.elasticsearch.test.autoconfigure.DataElasticsearchTest;
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

import java.time.Duration;
import java.util.Map;

import static de.medizininformatikinitiative.dataportal.backend.terminology.OntologyInfoContributor.KEY_ONTOLOGY_TAG_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Runs the check against real index mappings. The test context itself only loads if the strict check passes.
 */
@Tag("terminology")
@Tag("elasticsearch")
@Import({OntologyVersionCheck.class, PropertiesReader.class})
@Testcontainers
@DataElasticsearchTest(properties = {
    "app.elastic.ontologyVersionCheck=strict",
    "ontology-tag=" + OntologyVersionCheckIT.FIXTURE_VERSION
})
class OntologyVersionCheckIT {

  /**
   * The value of {@code _meta.version} in the mapping fixtures.
   */
  static final String FIXTURE_VERSION = "v0.0.0-test";

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
  private ElasticsearchClient client;

  @BeforeAll
  static void setUp() {
    ELASTICSEARCH_CONTAINER.start();
    var webClient = WebClient.builder().baseUrl("http://" + ELASTICSEARCH_CONTAINER.getHttpHostAddress()).build();
    Map.of("ontology", "ontology.json", "codeable_concept", "codeable_concept.json", "profile", "profile_mapping.json")
        .forEach((index, fixture) -> webClient.put()
            .uri("/" + index)
            .body(BodyInserters.fromResource(new ClassPathResource(fixture, OntologyVersionCheckIT.class)))
            .retrieve()
            .toBodilessEntity()
            .block());
  }

  @AfterAll
  static void tearDown() {
    ELASTICSEARCH_CONTAINER.stop();
  }

  private OntologyVersionCheck strictCheckExpecting(String version) {
    var propertiesReader = mock(PropertiesReader.class);
    doReturn(version).when(propertiesReader).getValue(KEY_ONTOLOGY_TAG_PROPERTIES);
    return new OntologyVersionCheck(client, propertiesReader, Mode.STRICT, 1, Duration.ZERO);
  }

  @Test
  void passesWhenAllIndicesHaveTheExpectedVersion() {
    assertThatCode(() -> strictCheckExpecting(FIXTURE_VERSION).afterSingletonsInstantiated())
        .doesNotThrowAnyException();
  }

  @Test
  void failsAndReportsAllIndicesWhenVersionDiffers() {
    assertThatThrownBy(() -> strictCheckExpecting("v9.9.9").afterSingletonsInstantiated())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("index 'ontology' has version " + FIXTURE_VERSION)
        .hasMessageContaining("index 'codeable_concept' has version " + FIXTURE_VERSION)
        .hasMessageContaining("index 'profile' has version " + FIXTURE_VERSION);
  }
}
