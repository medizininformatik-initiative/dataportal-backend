package de.medizininformatikinitiative.dataportal.backend.terminology.es;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileSearchEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileDisplay;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileDocument;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileField;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileRelative;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.ProfileEsRepository;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.ProfileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

  private String[] translatedQueryFields = new String[]{
      "display.localization.de-DE^2", "display.localization.en-US^2", "name^3",
      "fields.display.localization.de-DE", "fields.display.localization.en-US",
      "fields.display.localization.de-DE.ngram", "fields.display.localization.en-US.ngram",
      "display.localization.de-DE.ngram", "display.localization.en-US.ngram", "name.ngram"
  };
  private String[] originalQueryFields = new String[]{
      "display.original^2", "name^3", "fields.display.original", "fields.display.original.ngram", "name.ngram"
  };
  @Mock
  private ElasticsearchOperations operations;
  @Mock
  private ProfileEsRepository repo;
  @Mock
  private SearchHits<ProfileDocument> aggregationSearchHits;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ElasticsearchAggregations elasticsearchAggregations;

  private ProfileService profileService;

  private String[] pinnedProfileUrls = new String[]{};

  @BeforeEach
  void setUp() {
    Mockito.reset(operations, repo);
    profileService = new ProfileService(translatedQueryFields, originalQueryFields, pinnedProfileUrls, operations, repo);
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithoutFilters() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("foo", null, null, null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
    assertThat(result.getResults()).containsExactlyInAnyOrderElementsOf(
        dummySearchHitsPage.getSearchHits().stream().map(sh -> ProfileSearchEntry.of(sh.getContent())).toList());
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithModuleFilter() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(3);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("foo", List.of("Diagnose"), null, null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithCategoriesFilter() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(3);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("foo", null, List.of("category-a"), null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithResourceTypeFilter() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(3);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("foo", null, null, List.of("Condition"), 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithModuleAndCategoriesFilter() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(3);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging(
        "foo", List.of("Diagnose"), List.of("category-a"), List.of("Condition"), 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithEmptyKeyword() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("", null, null, null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isEqualTo(dummySearchHitsPage.getTotalHits());
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_succeedsWithEmptyResult() {
    SearchHits<ProfileDocument> dummySearchHitsPage = createDummySearchHitsPage(0);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> profileService.performProfileSearchWithRepoAndPaging("foo", null, null, null, 20, 0));

    assertThat(result).isNotNull();
    assertThat(result.getTotalHits()).isZero();
    assertThat(result.getResults()).isEmpty();
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_pinsConfiguredUrlsToTopWhenKeywordEmpty() {
    var pinnedUrls = new String[]{"https://example.org/pin-1", "https://example.org/pin-2"};
    var service = new ProfileService(translatedQueryFields, originalQueryFields, pinnedUrls, operations, repo);
    mockPinnedSearch(List.of(
        "https://example.org/pin-2", "https://example.org/other-1",
        "https://example.org/pin-1", "https://example.org/other-2"));

    var result = assertDoesNotThrow(() -> service.performProfileSearchWithRepoAndPaging("", null, null, null, 20, 0));

    assertThat(result.getTotalHits()).isEqualTo(4L);
    assertThat(result.getResults()).extracting("url").containsExactly(
        "https://example.org/pin-1", "https://example.org/pin-2",
        "https://example.org/other-1", "https://example.org/other-2");
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_pinnedSearchFetchesUrlsFirstAndThenPageByIdsWithoutFields() {
    var pinnedUrls = new String[]{"https://example.org/pin-1"};
    var service = new ProfileService(translatedQueryFields, originalQueryFields, pinnedUrls, operations, repo);
    mockPinnedSearch(List.of(
        "https://example.org/other-1", "https://example.org/pin-1", "https://example.org/other-2"));

    var result = assertDoesNotThrow(() -> service.performProfileSearchWithRepoAndPaging("", null, null, null, 2, 0));

    assertThat(result.getTotalHits()).isEqualTo(3L);
    var queries = captureSearchQueries(2);

    var urlQuery = queries.get(0);
    assertThat(urlQuery.getQuery().isBool()).isTrue();
    assertThat(urlQuery.getPageable().getPageSize()).isEqualTo(10000);
    assertThat(urlQuery.getSourceFilter().getIncludes()).containsExactly("url", "selectable");

    var pageQuery = queries.get(1);
    assertThat(pageQuery.getQuery().isIds()).isTrue();
    assertThat(pageQuery.getQuery().ids().values()).containsExactly(idForUrl("https://example.org/pin-1"),
        idForUrl("https://example.org/other-1"));
    assertThat(pageQuery.getPageable().getPageSize()).isEqualTo(2);
    assertThat(pageQuery.getSourceFilter().getExcludes()).containsExactly("fields");
    assertThat(pageQuery.getSourceFilter().getIncludes()).isNullOrEmpty();
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_pinnedSearchRestoresSliceOrderOfIdsQueryHits() {
    var pinnedUrls = new String[]{"https://example.org/pin-1", "https://example.org/pin-2", "https://example.org/pin-3"};
    var service = new ProfileService(translatedQueryFields, originalQueryFields, pinnedUrls, operations, repo);
    // mockPinnedSearch answers the ids query in reverse order
    mockPinnedSearch(List.of(
        "https://example.org/other-1", "https://example.org/pin-3", "https://example.org/pin-1",
        "https://example.org/other-2", "https://example.org/pin-2"));

    var result = assertDoesNotThrow(() -> service.performProfileSearchWithRepoAndPaging("", null, null, null, 4, 0));

    assertThat(result.getTotalHits()).isEqualTo(5L);
    assertThat(result.getResults()).extracting("url").containsExactly(
        "https://example.org/pin-1", "https://example.org/pin-2", "https://example.org/pin-3",
        "https://example.org/other-1");
    assertThat(result.getResults()).extracting("id").containsExactly(
        idForUrl("https://example.org/pin-1"), idForUrl("https://example.org/pin-2"),
        idForUrl("https://example.org/pin-3"), idForUrl("https://example.org/other-1"));
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_pinnedSearchReturnsPartialLastPage() {
    var pinnedUrls = new String[]{"https://example.org/pin-1"};
    var service = new ProfileService(translatedQueryFields, originalQueryFields, pinnedUrls, operations, repo);
    mockPinnedSearch(List.of(
        "https://example.org/other-1", "https://example.org/pin-1", "https://example.org/other-2"));

    var result = assertDoesNotThrow(() -> service.performProfileSearchWithRepoAndPaging("", null, null, null, 2, 1));

    assertThat(result.getTotalHits()).isEqualTo(3L);
    assertThat(result.getResults()).extracting("url").containsExactly("https://example.org/other-2");
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_pinnedSearchSkipsIdsQueryForPageBeyondEnd() {
    var pinnedUrls = new String[]{"https://example.org/pin-1"};
    var service = new ProfileService(translatedQueryFields, originalQueryFields, pinnedUrls, operations, repo);
    mockPinnedSearch(List.of("https://example.org/other-1", "https://example.org/pin-1"));

    var result = assertDoesNotThrow(() -> service.performProfileSearchWithRepoAndPaging("", null, null, null, 2, 5));

    assertThat(result.getTotalHits()).isEqualTo(2L);
    assertThat(result.getResults()).isEmpty();
    assertThat(captureSearchQueries(1).get(0).getQuery().isIds()).isFalse();
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_pinnedSearchSkipsIdsQueryWhenNothingMatches() {
    var pinnedUrls = new String[]{"https://example.org/pin-1"};
    var service = new ProfileService(translatedQueryFields, originalQueryFields, pinnedUrls, operations, repo);
    mockPinnedSearch(List.of());

    var result = assertDoesNotThrow(() -> service.performProfileSearchWithRepoAndPaging("", List.of("Diagnose"), null, null, 20, 0));

    assertThat(result.getTotalHits()).isZero();
    assertThat(result.getResults()).isEmpty();
    captureSearchQueries(1);
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_pinnedSearchAppliesFiltersToUrlQuery() {
    var pinnedUrls = new String[]{"https://example.org/pin-1"};
    var service = new ProfileService(translatedQueryFields, originalQueryFields, pinnedUrls, operations, repo);
    // pin-1 does not match the active filters, so the filtered url query does not return it
    mockPinnedSearch(List.of("https://example.org/other-1"));

    var result = assertDoesNotThrow(() -> service.performProfileSearchWithRepoAndPaging(
        "", List.of("Diagnose"), List.of("category-a"), List.of("Condition"), 20, 0));

    assertThat(result.getTotalHits()).isEqualTo(1L);
    assertThat(result.getResults()).extracting("url").containsExactly("https://example.org/other-1");

    var urlQuery = captureSearchQueries(2).get(0);
    assertThat(urlQuery.getQuery().bool().filter())
        .filteredOn(Query::isTerms)
        .extracting(q -> q.terms().field())
        .containsExactly(ProfileService.FILTER_KEY_MODULE, ProfileService.FILTER_KEY_CATEGORIES,
            ProfileService.FILTER_KEY_RESOURCE_TYPE);
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_doesNotPinWhenKeywordPresent() {
    var pinnedUrls = new String[]{"https://example.org/pin-1"};
    var service = new ProfileService(translatedQueryFields, originalQueryFields, pinnedUrls, operations, repo);
    var hits = createDummySearchHitsPageWithUrls(List.of(
        "https://example.org/other-1", "https://example.org/pin-1"));
    doReturn(hits).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> service.performProfileSearchWithRepoAndPaging("foo", null, null, null, 20, 0));

    assertThat(result.getResults()).extracting("url").containsExactly(
        "https://example.org/other-1", "https://example.org/pin-1");
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_pinnedSearchIsPagedManually() {
    var pinnedUrls = new String[]{"https://example.org/pin-1", "https://example.org/pin-2"};
    var service = new ProfileService(translatedQueryFields, originalQueryFields, pinnedUrls, operations, repo);
    mockPinnedSearch(List.of(
        "https://example.org/other-1", "https://example.org/pin-2",
        "https://example.org/pin-1", "https://example.org/other-2"));

    var firstPage = assertDoesNotThrow(() -> service.performProfileSearchWithRepoAndPaging("", null, null, null, 2, 0));
    var secondPage = assertDoesNotThrow(() -> service.performProfileSearchWithRepoAndPaging("", null, null, null, 2, 1));

    assertThat(firstPage.getTotalHits()).isEqualTo(4L);
    assertThat(firstPage.getResults()).extracting("url").containsExactly(
        "https://example.org/pin-1", "https://example.org/pin-2");
    assertThat(secondPage.getTotalHits()).isEqualTo(4L);
    assertThat(secondPage.getResults()).extracting("url").containsExactly(
        "https://example.org/other-1", "https://example.org/other-2");
  }

  @Test
  void testPerformProfileSearchWithRepoAndPaging_pinnedUrlNotInResultsIsIgnored() {
    var pinnedUrls = new String[]{"https://example.org/pin-1", "https://example.org/not-present"};
    var service = new ProfileService(translatedQueryFields, originalQueryFields, pinnedUrls, operations, repo);
    mockPinnedSearch(List.of("https://example.org/other-1", "https://example.org/pin-1"));

    var result = assertDoesNotThrow(() -> service.performProfileSearchWithRepoAndPaging("", null, null, null, 20, 0));

    assertThat(result.getTotalHits()).isEqualTo(2L);
    assertThat(result.getResults()).extracting("url").containsExactly(
        "https://example.org/pin-1", "https://example.org/other-1");
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
    assertThat(result.resourceType()).isNotNull();
    assertThat(result.resourceType().display().original()).isEqualTo("Some Name");
    assertThat(result.fields()).hasSize(1);
    assertThat(result.fields().get(0).display().original()).isEqualTo("Some Name");
    assertThat(result.parents()).hasSize(1);
    assertThat(result.parents().get(0).url()).isEqualTo("https://example.org/relative");
    assertThat(result.parents().get(0).selectable()).isTrue();
    assertThat(result.children()).hasSize(2);
    assertThat(result.children().get(0).url()).isEqualTo("https://example.org/relative");
    assertThat(result.children().get(0).selectable()).isTrue();
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
        .categories(Arrays.asList(null, createDummyDisplay()))
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
        .resourceType(null)
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
    assertThat(result.resourceType()).isNull();
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

  @Test
  void testGetAvailableFiltersTargeted_appliesCategoriesFilterWhenTargetIsNotCategory() {
    mockEmptyAggregationBuckets();

    var result = assertDoesNotThrow(() -> profileService.getAvailableFilters(
        ProfileService.FILTER_NAME_MODULE, null, null, List.of("category-a"), null));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo(ProfileService.FILTER_NAME_MODULE);
    assertThat(result.get(0).values()).isEmpty();
  }

  @Test
  void testGetAvailableFiltersTargeted_appliesResourceTypeFilterWhenTargetIsNotResourceType() {
    mockEmptyAggregationBuckets();

    var result = assertDoesNotThrow(() -> profileService.getAvailableFilters(
        ProfileService.FILTER_NAME_MODULE, null, null, null, List.of("Condition")));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo(ProfileService.FILTER_NAME_MODULE);
    assertThat(result.get(0).values()).isEmpty();
  }

  @Test
  void testGetAvailableFilters_resolvesDefaultDisplayWhenSampleHasNoHits() {
    var bucketWithoutSampleHits = new StringTermsBucket.Builder()
        .key("some-key")
        .docCount(1)
        .aggregations("sample", Aggregate.of(a -> a.topHits(th -> th.hits(hm -> hm.hits(List.of())))))
        .build();

    doReturn(aggregationSearchHits).when(operations).search(any(NativeQuery.class), any());
    doReturn(elasticsearchAggregations).when(aggregationSearchHits).getAggregations();
    when(elasticsearchAggregations.aggregationsAsMap().get(any(String.class)).aggregation().getAggregate().sterms()
        .buckets().array()).thenReturn(List.of(bucketWithoutSampleHits));

    var result = assertDoesNotThrow(() -> profileService.getAvailableFilters());

    var moduleFilter = result.stream().filter(f -> f.name().equals(ProfileService.FILTER_NAME_MODULE)).findFirst().orElseThrow();
    assertThat(moduleFilter.values()).hasSize(1);
    assertThat(moduleFilter.values().get(0).display().original()).isEqualTo("some-key");
    assertThat(moduleFilter.values().get(0).display().translations()).isEmpty();
  }

  @Test
  void testKeyFieldFor_throwsOnUnknownFilterName() {
    assertThrows(IllegalArgumentException.class, () -> invokePrivateStatic("keyFieldFor", "unknown"));
  }

  @Test
  void testBaseFieldFor_throwsOnUnknownFilterName() {
    assertThrows(IllegalArgumentException.class, () -> invokePrivateStatic("baseFieldFor", "unknown"));
  }

  private void mockEmptyAggregationBuckets() {
    doReturn(aggregationSearchHits).when(operations).search(any(NativeQuery.class), any());
    doReturn(elasticsearchAggregations).when(aggregationSearchHits).getAggregations();
    when(elasticsearchAggregations.aggregationsAsMap().get(any(String.class)).aggregation().getAggregate().sterms()
        .buckets().array()).thenReturn(List.of());
  }

  private static void invokePrivateStatic(String methodName, String arg) throws Throwable {
    var method = ProfileService.class.getDeclaredMethod(methodName, String.class);
    method.setAccessible(true);
    try {
      method.invoke(null, arg);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
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

  private SearchHits<ProfileDocument> createDummySearchHitsPageWithUrls(List<String> urls) {
    return createSearchHits(urls.stream()
        .map(url -> ProfileDocument.builder()
            .id(UUID.randomUUID().toString())
            .name("some-profile")
            .display(createDummyDisplay())
            .selectable(true)
            .url(url)
            .build())
        .toList());
  }

  private SearchHits<ProfileDocument> createSearchHits(List<ProfileDocument> documents) {
    var searchHitsList = new ArrayList<SearchHit<ProfileDocument>>();
    documents.forEach(document ->
        searchHitsList.add(new SearchHit<>(null, null, null, 10.0F, null, null, null, null, null, null, document)));
    return new SearchHitsImpl<>(documents.size(), TotalHitsRelation.OFF, 10.0F, null, null, null, searchHitsList, null, null, null);
  }

  private static String idForUrl(String url) {
    return "id-" + url;
  }

  /**
   * Simulates the two queries of the pinned profile search: the url query returns id and url of every given
   * profile in the given order, the ids query returns the full documents of the requested ids in reverse order
   * (an ids query does not guarantee any order).
   */
  private void mockPinnedSearch(List<String> urlsInIndexOrder) {
    var fullDocuments = urlsInIndexOrder.stream()
        .map(url -> ProfileDocument.builder()
            .id(idForUrl(url))
            .name("some-profile")
            .display(createDummyDisplay())
            .selectable(true)
            .url(url)
            .build())
        .toList();

    doAnswer(invocation -> {
      NativeQuery query = invocation.getArgument(0);
      if (query.getQuery() != null && query.getQuery().isIds()) {
        var requestedIds = query.getQuery().ids().values();
        var matching = new ArrayList<>(fullDocuments.stream().filter(d -> requestedIds.contains(d.id())).toList());
        Collections.reverse(matching);
        return createSearchHits(matching);
      }
      return createSearchHits(fullDocuments.stream()
          .map(d -> ProfileDocument.builder().id(d.id()).url(d.url()).build())
          .toList());
    }).when(operations).search(any(NativeQuery.class), any(Class.class));
  }

  private List<NativeQuery> captureSearchQueries(int expectedCount) {
    var captor = ArgumentCaptor.forClass(NativeQuery.class);
    verify(operations, times(expectedCount)).search(captor.capture(), any(Class.class));
    return captor.getAllValues();
  }

  private ProfileDocument createDummyProfileDocument(String id, List<ProfileRelative> parents, List<ProfileRelative> children) {
    return ProfileDocument.builder()
        .id(id)
        .name("some-profile")
        .display(createDummyDisplay())
        .description(List.of(createDummyDisplay()))
        .selectable(true)
        .url("https://example.org/some-profile")
        .module(createDummyDisplay())
        .resourceType(createDummyDisplay())
        .categories(List.of(createDummyDisplay()))
        .availability(1)
        .fields(List.of(createDummyField()))
        .parents(parents)
        .children(children)
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
        .selectable(true)
        .build();
  }

  private ProfileDisplay createDummyDisplay() {
    return ProfileDisplay.builder()
        .original("Some Name")
        .translations(Map.of("de-DE", "Some German Name", "en-US", "Some English Name"))
        .build();
  }
}
