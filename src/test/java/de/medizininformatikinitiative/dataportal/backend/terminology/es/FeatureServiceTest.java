package de.medizininformatikinitiative.dataportal.backend.terminology.es;

import de.medizininformatikinitiative.dataportal.backend.terminology.api.FeatureSearchEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.Display;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.FeatureDocument;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.FeatureField;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.FeatureModule;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.FeatureRelative;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.FeatureEsRepository;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.FeatureNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

  @Mock
  private ElasticsearchOperations operations;
  @Mock
  private FeatureEsRepository repo;

  private FeatureService featureService;

  @BeforeEach
  void setUp() {
    Mockito.reset(operations, repo);
    featureService = new FeatureService(operations, repo);
  }

  @Test
  void testPerformFeatureSearchWithRepoAndPaging_succeedsWithoutFilters() {
    SearchHits<FeatureDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> featureService.performFeatureSearchWithRepoAndPaging("foo", null, null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
    assertThat(result.getResults()).containsExactlyInAnyOrderElementsOf(
        dummySearchHitsPage.getSearchHits().stream().map(sh -> FeatureSearchEntry.of(sh.getContent())).toList());
  }

  @Test
  void testPerformFeatureSearchWithRepoAndPaging_succeedsWithModuleFilter() {
    SearchHits<FeatureDocument> dummySearchHitsPage = createDummySearchHitsPage(3);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> featureService.performFeatureSearchWithRepoAndPaging("foo", List.of("Diagnose"), null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformFeatureSearchWithRepoAndPaging_succeedsWithCategoriesFilter() {
    SearchHits<FeatureDocument> dummySearchHitsPage = createDummySearchHitsPage(3);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> featureService.performFeatureSearchWithRepoAndPaging("foo", null, List.of("category-a"), 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformFeatureSearchWithRepoAndPaging_succeedsWithModuleAndCategoriesFilter() {
    SearchHits<FeatureDocument> dummySearchHitsPage = createDummySearchHitsPage(3);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> featureService.performFeatureSearchWithRepoAndPaging(
        "foo", List.of("Diagnose"), List.of("category-a"), 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformFeatureSearchWithRepoAndPaging_succeedsWithEmptyKeyword() {
    SearchHits<FeatureDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> featureService.performFeatureSearchWithRepoAndPaging("", null, null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformFeatureSearchWithRepoAndPaging_succeedsWithEmptyResult() {
    SearchHits<FeatureDocument> dummySearchHitsPage = createDummySearchHitsPage(0);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> featureService.performFeatureSearchWithRepoAndPaging("foo", null, null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isZero();
    assertThat(result.getResults()).isEmpty();
  }

  @Test
  void testGetFeatureListDetailsById_succeedsWithRelations() {
    String id = UUID.randomUUID().toString();
    var dummyDocument = createDummyFeatureDocument(id, List.of(createDummyRelative()), List.of(createDummyRelative(), createDummyRelative()));
    doReturn(Optional.of(dummyDocument)).when(repo).findById(id);

    var result = assertDoesNotThrow(() -> featureService.getFeatureListDetailsById(id));

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(id);
    assertThat(result.description()).isNotNull();
    assertThat(result.fields()).hasSize(1);
    assertThat(result.fields().get(0).original()).isEqualTo("Some Name");
    assertThat(result.parents()).hasSize(1);
    assertThat(result.parents().get(0).url()).isEqualTo("https://example.org/relative");
    assertThat(result.children()).hasSize(2);
    assertThat(result.children().get(0).url()).isEqualTo("https://example.org/relative");
  }

  @Test
  void testGetFeatureListDetailsById_succeedsWithoutRelations() {
    String id = UUID.randomUUID().toString();
    var dummyDocument = createDummyFeatureDocument(id, null, null);
    doReturn(Optional.of(dummyDocument)).when(repo).findById(id);

    var result = assertDoesNotThrow(() -> featureService.getFeatureListDetailsById(id));

    assertThat(result).isNotNull();
    assertThat(result.parents()).isNotNull().isEmpty();
    assertThat(result.children()).isNotNull().isEmpty();
  }

  @Test
  void testGetFeatureListDetailsById_succeedsWithoutDescriptionAndModule() {
    String id = UUID.randomUUID().toString();
    var dummyDocument = FeatureDocument.builder()
        .id(id)
        .name("some-feature")
        .display(createDummyDisplay())
        .description(null)
        .selectable(true)
        .url("some-url")
        .module(null)
        .categories(null)
        .availability(null)
        .fields(null)
        .parents(null)
        .children(null)
        .build();
    doReturn(Optional.of(dummyDocument)).when(repo).findById(id);

    var result = assertDoesNotThrow(() -> featureService.getFeatureListDetailsById(id));

    assertThat(result).isNotNull();
    assertThat(result.description()).isNull();
    assertThat(result.module()).isNull();
    assertThat(result.categories()).isEmpty();
    assertThat(result.availability()).isZero();
    assertThat(result.fields()).isEmpty();
    assertThat(result.parents()).isEmpty();
    assertThat(result.children()).isEmpty();
  }

  @Test
  void testGetFeatureListDetailsById_throwsOnNotFound() {
    doReturn(Optional.empty()).when(repo).findById("id");

    assertThrows(FeatureNotFoundException.class, () -> featureService.getFeatureListDetailsById("id"));
  }

  private SearchHits<FeatureDocument> createDummySearchHitsPage(int totalHits) {
    var searchHitsList = new ArrayList<SearchHit<FeatureDocument>>();

    for (int i = 0; i < totalHits; ++i) {
      searchHitsList.add(
          new SearchHit<>(
              null,
              null,
              null,
              10.0F,
              null,
              null,
              null,
              null,
              null,
              null,
              createDummyFeatureDocument(UUID.randomUUID().toString(), null, null)
          )
      );
    }
    return new SearchHitsImpl<>(totalHits, TotalHitsRelation.OFF, 10.0F, null, null, null, searchHitsList, null, null, null);
  }

  private FeatureDocument createDummyFeatureDocument(String id, List<FeatureRelative> parents, List<FeatureRelative> children) {
    return FeatureDocument.builder()
        .id(id)
        .name("some-feature")
        .display(createDummyDisplay())
        .description(createDummyDisplay())
        .selectable(true)
        .url("https://example.org/some-feature")
        .module(FeatureModule.builder().display(createDummyDisplay()).build())
        .categories(List.of("category-a"))
        .availability(1)
        .fields(List.of(createDummyField()))
        .parents(parents)
        .children(children)
        .build();
  }

  private FeatureField createDummyField() {
    return FeatureField.builder()
        .display(createDummyDisplay())
        .build();
  }

  private FeatureRelative createDummyRelative() {
    return FeatureRelative.builder()
        .id(UUID.randomUUID().toString())
        .display(createDummyDisplay())
        .url("https://example.org/relative")
        .build();
  }

  private Display createDummyDisplay() {
    return Display.builder()
        .original("Some Name")
        .deDe("Some German Name")
        .enUs("Some English Name")
        .build();
  }
}
