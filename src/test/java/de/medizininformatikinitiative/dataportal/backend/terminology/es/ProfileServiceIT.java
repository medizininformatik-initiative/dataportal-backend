package de.medizininformatikinitiative.dataportal.backend.terminology.es;

import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.ProfileEsRepository;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.ProfileNotFoundException;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("terminology")
@Tag("elasticsearch")
@Import({ProfileService.class})
@Testcontainers
@DataElasticsearchTest
public class ProfileServiceIT {

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
  private ProfileEsRepository repo;
  @Autowired
  private ProfileService profileService;

  @BeforeAll
  static void setUp() throws InterruptedException {
    ELASTICSEARCH_CONTAINER.start();
    WebClient webClient = WebClient.builder().baseUrl("http://" + ELASTICSEARCH_CONTAINER.getHttpHostAddress()).build();
    webClient.put()
        .uri("/profile")
        .body(BodyInserters.fromResource(new ClassPathResource("profile_mapping.json", ProfileServiceIT.class)))
        .retrieve()
        .toBodilessEntity()
        .block();

    webClient.post()
        .uri("/profile/_bulk")
        .body(BodyInserters.fromResource(new ClassPathResource("profile_testdata.json", ProfileServiceIT.class)))
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
  void testPerformProfileSearchWithRepoAndPaging_findsAllWithNoKeyword() {
    var page = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("", null, null, null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(1L);
    assertThat(page.getResults().get(0).id()).isEqualTo("diagnose-condition-id");
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_findsNone() {
    var page = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("something-not-found", null, null, null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isZero();
    assertThat(page.getResults()).isEmpty();
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_findsByOriginalDisplay() {
    var page = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("Condition", null, null, null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(1L);
    assertThat(page.getResults().get(0).id()).isEqualTo("diagnose-condition-id");
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_excludesNonSelectableEntries() {
    var page = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("Prozedur", null, null, null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isZero();
    assertThat(page.getResults()).isEmpty();
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_findsByTranslatedField() {
    var page = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("Feststellungsdatum", null, null, null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(1L);
    assertThat(page.getResults().get(0).id()).isEqualTo("diagnose-condition-id");
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_filtersByModule() {
    var page = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("", List.of("Diagnose"), null, null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(1L);
    assertThat(page.getResults().get(0).id()).isEqualTo("diagnose-condition-id");
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_filtersByCategories() {
    var page = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("", null, List.of("element"), null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(1L);
    assertThat(page.getResults().get(0).id()).isEqualTo("diagnose-condition-id");
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_filtersByResourceType() {
    var page = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("", null, null, List.of("Condition"), 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(1L);
    assertThat(page.getResults().get(0).id()).isEqualTo("diagnose-condition-id");
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_filtersByCategories_excludesNonSelectableEntries() {
    var page = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("", null, List.of("module"), null, 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isZero();
    assertThat(page.getResults()).isEmpty();
  }

  @Test
  void testGetProfileListDetailsById_succeedsWithChildrenOnly() {
    var result = assertDoesNotThrow(() -> profileService.getProfileListDetailsById("module-diagnose-id"));

    assertNotNull(result);
    assertThat(result.id()).isEqualTo("module-diagnose-id");
    assertThat(result.description()).isNull();
    assertThat(result.fields()).isEmpty();
    assertThat(result.parents()).isEmpty();
    assertThat(result.children()).hasSize(1);
    assertThat(result.children().get(0).id()).isEqualTo("diagnose-condition-id");
    assertThat(result.children().get(0).url()).isEqualTo("https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose");
    assertThat(result.children().get(0).selectable()).isTrue();
  }

  @Test
  void testGetProfileListDetailsById_succeedsWithParentsOnly() {
    var result = assertDoesNotThrow(() -> profileService.getProfileListDetailsById("diagnose-condition-id"));

    assertNotNull(result);
    assertThat(result.id()).isEqualTo("diagnose-condition-id");
    assertThat(result.description()).isNotNull();
    assertThat(result.description().display().original()).isEqualTo("Diagnose Beschreibung");
    assertThat(result.resourceType()).isNotNull();
    assertThat(result.resourceType().display().original()).isEqualTo("Condition");
    assertThat(result.fields()).hasSize(2);
    assertThat(result.fields()).extracting("display.original").containsExactlyInAnyOrder("recordedDate", "code");
    assertThat(result.parents()).hasSize(1);
    assertThat(result.parents().get(0).id()).isEqualTo("module-diagnose-id");
    assertThat(result.parents().get(0).url()).isEqualTo("modul-diagnose");
    assertThat(result.parents().get(0).selectable()).isFalse();
    assertThat(result.children()).isEmpty();
  }

  @Test
  void testGetProfileListDetailsById_succeedsWithoutAnyRelations() {
    var result = assertDoesNotThrow(() -> profileService.getProfileListDetailsById("module-prozedur-id"));

    assertNotNull(result);
    assertThat(result.parents()).isEmpty();
    assertThat(result.children()).isEmpty();
  }

  @Test
  void testGetProfileListDetailsById_throwsOnNotFound() {
    assertThrows(ProfileNotFoundException.class, () -> profileService.getProfileListDetailsById("does-not-exist"));
  }

  @Test
  void testGetAvailableFilters_returnsModuleAndCategoryCounts() {
    var filters = assertDoesNotThrow(() -> profileService.getAvailableFilters());

    assertNotNull(filters);
    assertThat(filters).extracting("name").containsExactlyInAnyOrder("module", "category", "resourceType");

    var moduleFilter = filters.stream().filter(f -> f.name().equals("module")).findFirst().orElseThrow();
    assertThat(moduleFilter.values()).extracting("display.original").containsExactlyInAnyOrder("Diagnose", "Prozedur");
    assertThat(moduleFilter.values()).filteredOn(v -> v.display().original().equals("Diagnose")).extracting("count").containsExactly(2L);
    assertThat(moduleFilter.values()).filteredOn(v -> v.display().original().equals("Diagnose"))
        .flatExtracting("display.translations").extracting("language", "value")
        .containsExactlyInAnyOrder(tuple("de-DE", "Diagnose"), tuple("en-US", "Diagnosis"));

    var categoryFilter = filters.stream().filter(f -> f.name().equals("category")).findFirst().orElseThrow();
    assertThat(categoryFilter.values()).extracting("display.original").containsExactlyInAnyOrder("module", "element");

    var resourceTypeFilter = filters.stream().filter(f -> f.name().equals("resourceType")).findFirst().orElseThrow();
    assertThat(resourceTypeFilter.values()).extracting("display.original").containsExactly("Condition");
    assertThat(resourceTypeFilter.values()).filteredOn(v -> v.display().original().equals("Condition")).extracting("count").containsExactly(1L);
  }

  @Test
  void testGetAvailableFilters_withTargetFilterReturnsOnlyThatFilter() {
    var filters = assertDoesNotThrow(() -> profileService.getAvailableFilters("resourceType", null, null, null, null));

    assertNotNull(filters);
    assertThat(filters).hasSize(1);
    assertThat(filters.get(0).name()).isEqualTo("resourceType");
    assertThat(filters.get(0).values()).extracting("display.original").containsExactly("Condition");
  }

  @Test
  void testGetAvailableFilters_withTargetFilterAndCrossFilterNarrowsCounts() {
    var filters = assertDoesNotThrow(() -> profileService.getAvailableFilters("category", null, List.of("Prozedur"), null, null));

    assertNotNull(filters);
    assertThat(filters).hasSize(1);
    assertThat(filters.get(0).name()).isEqualTo("category");
    assertThat(filters.get(0).values()).extracting("display.original").containsExactly("module");
  }

  @Test
  void testGetAvailableFilters_withTargetFilterAndSearchtermNarrowsCounts() {
    var filters = assertDoesNotThrow(() -> profileService.getAvailableFilters("module", "Feststellungsdatum", null, null, null));

    assertNotNull(filters);
    assertThat(filters).hasSize(1);
    assertThat(filters.get(0).name()).isEqualTo("module");
    assertThat(filters.get(0).values()).extracting("display.original").containsExactly("Diagnose");
  }

  @Test
  void testGetAvailableFilters_withUnknownTargetFilterThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> profileService.getAvailableFilters("not-a-filter", null, null, null, null));
  }
}
