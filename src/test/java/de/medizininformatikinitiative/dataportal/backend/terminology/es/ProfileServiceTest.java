package de.medizininformatikinitiative.dataportal.backend.terminology.es;

import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileSearchEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileCategory;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileDisplay;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileDocument;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileField;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileLocalization;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileModule;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileRelative;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.ProfileEsRepository;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.ProfileNotFoundException;
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
class ProfileServiceTest {

  @Mock
  private ElasticsearchOperations operations;
  @Mock
  private ProfileEsRepository repo;

  private ProfileService profileService;

  @BeforeEach
  void setUp() {
    Mockito.reset(operations, repo);
    profileService = new ProfileService(operations, repo);
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithoutFilters() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("foo", null, null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
    assertThat(result.getResults()).containsExactlyInAnyOrderElementsOf(
        dummySearchHitsPage.getSearchHits().stream().map(sh -> ProfileSearchEntry.of(sh.getContent())).toList());
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithModuleFilter() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(3);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("foo", List.of("Diagnose"), null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithCategoriesFilter() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(3);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("foo", null, List.of("category-a"), 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithModuleAndCategoriesFilter() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(3);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging(
        "foo", List.of("Diagnose"), List.of("category-a"), 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithEmptyKeyword() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("", null, null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithEmptyResult() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(0);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("foo", null, null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isZero();
    assertThat(result.getResults()).isEmpty();
  }

  @Test
  void testGetProfileListDetailsById_succeedsWithRelations() {
    String id = UUID.randomUUID().toString();
    var dummyDocument = createDummyProfileDocument(id, List.of(createDummyRelative()), List.of(createDummyRelative(), createDummyRelative()));
    doReturn(Optional.of(dummyDocument)).when(repo).findById(id);

    var result = assertDoesNotThrow(() -> profileService.getProfileListDetailsById(id));

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
  void testGetProfileListDetailsById_succeedsWithCategoryAndFieldMissingDisplay() {
    String id = UUID.randomUUID().toString();
    var dummyDocument = ProfileDocument.builder()
        .id(id)
        .name("some-profile")
        .display(createDummyDisplay())
        .selectable(true)
        .url("https://example.org/some-profile")
        .categories(List.of(ProfileCategory.builder().display(null).build(), createDummyCategory()))
        .fields(List.of(ProfileField.builder().display(null).build(), createDummyField()))
        .build();
    doReturn(Optional.of(dummyDocument)).when(repo).findById(id);

    var result = assertDoesNotThrow(() -> profileService.getProfileListDetailsById(id));

    assertThat(result).isNotNull();
    assertThat(result.categories()).hasSize(1);
    assertThat(result.fields()).hasSize(1);
  }

  @Test
  void testGetProfileListDetailsById_succeedsWithoutRelations() {
    String id = UUID.randomUUID().toString();
    var dummyDocument = createDummyProfileDocument(id, null, null);
    doReturn(Optional.of(dummyDocument)).when(repo).findById(id);

    var result = assertDoesNotThrow(() -> profileService.getProfileListDetailsById(id));

    assertThat(result).isNotNull();
    assertThat(result.parents()).isNotNull().isEmpty();
    assertThat(result.children()).isNotNull().isEmpty();
  }

  @Test
  void testGetProfileListDetailsById_succeedsWithoutDescriptionAndModule() {
    String id = UUID.randomUUID().toString();
    var dummyDocument = ProfileDocument.builder()
        .id(id)
        .name("some-profile")
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

    var result = assertDoesNotThrow(() -> profileService.getProfileListDetailsById(id));

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
  void testGetProfileListDetailsById_throwsOnNotFound() {
    doReturn(Optional.empty()).when(repo).findById("id");

    assertThrows(ProfileNotFoundException.class, () -> profileService.getProfileListDetailsById("id"));
  }

  private SearchHits<ProfileDocument> createDummySearchHitsPage(int totalHits) {
    var searchHitsList = new ArrayList<SearchHit<ProfileDocument>>();

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
              createDummyProfileDocument(UUID.randomUUID().toString(), null, null)
          )
      );
    }
    return new SearchHitsImpl<>(totalHits, TotalHitsRelation.OFF, 10.0F, null, null, null, searchHitsList, null, null, null);
  }

  private ProfileDocument createDummyProfileDocument(String id, List<ProfileRelative> parents, List<ProfileRelative> children) {
    return ProfileDocument.builder()
        .id(id)
        .name("some-profile")
        .display(createDummyDisplay())
        .description(createDummyDisplay())
        .selectable(true)
        .url("https://example.org/some-profile")
        .module(ProfileModule.builder().display(createDummyDisplay()).build())
        .categories(List.of(createDummyCategory()))
        .availability(1)
        .fields(List.of(createDummyField()))
        .parents(parents)
        .children(children)
        .build();
  }

  private ProfileCategory createDummyCategory() {
    return ProfileCategory.builder()
        .display(createDummyDisplay())
        .build();
  }

  private ProfileField createDummyField() {
    return ProfileField.builder()
        .display(createDummyDisplay())
        .build();
  }

  private ProfileRelative createDummyRelative() {
    return ProfileRelative.builder()
        .id(UUID.randomUUID().toString())
        .display(createDummyDisplay())
        .url("https://example.org/relative")
        .build();
  }

  private ProfileDisplay createDummyDisplay() {
    return ProfileDisplay.builder()
        .original("Some Name")
        .localization(ProfileLocalization.builder()
            .deDe("Some German Name")
            .enUs("Some English Name")
            .build())
        .build();
  }
}
