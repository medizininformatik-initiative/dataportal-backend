package de.numcodex.feasibility_gui_backend.terminology.es;

import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntryExtended;
import de.numcodex.feasibility_gui_backend.terminology.api.RelativeEntry;
import de.numcodex.feasibility_gui_backend.terminology.api.TerminologyBulkSearchRequest;
import de.numcodex.feasibility_gui_backend.terminology.es.model.*;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemEsRepository;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemNotFoundException;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyListItemEsRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@Tag("terminology")
@Tag("elasticsearch")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TerminologyEsServiceTest {

  private String[] filterFields = new String[] {"foo", "bar", "baz"};
  @Mock
  private ElasticsearchOperations operations;
  @Mock
  private OntologyItemEsRepository ontologyItemEsRepository;
  @Mock
  private OntologyListItemEsRepository ontologyListItemEsRepository;
  @Mock
  private SearchHits<OntologyListItemDocument> searchHits;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ElasticsearchAggregations elasticsearchAggregations;
  @Mock
  private List<StringTermsBucket> stringTermsBuckets;

  @InjectMocks
  private TerminologyEsService terminologyEsService;

  private TerminologyEsService createTerminologyEsService() {
    return new TerminologyEsService(filterFields, operations, ontologyItemEsRepository, ontologyListItemEsRepository);
  }

  @BeforeEach
  void setUp() {
    Mockito.reset(operations, ontologyItemEsRepository, ontologyListItemEsRepository);
    terminologyEsService = createTerminologyEsService();
  }

  @Test
  void testGetSearchResultEntryByHash() {
    String id = UUID.randomUUID().toString();
    OntologyListItemDocument dummyOntologyListItem = createDummyOntologyListItem(id);
    doReturn(Optional.of(dummyOntologyListItem)).when(ontologyListItemEsRepository).findById(any(String.class));

    var searchResultEntry = assertDoesNotThrow(() -> terminologyEsService.getSearchResultEntryByHash(id));

    assertThat(searchResultEntry).isNotNull();
    assertThat(searchResultEntry.id()).isEqualTo(id);
    assertThat(searchResultEntry.terminology()).isEqualTo(dummyOntologyListItem.terminology());
    assertThat(searchResultEntry.display().original()).isEqualTo(dummyOntologyListItem.display().original());
    assertThat(searchResultEntry.kdsModule()).isEqualTo(dummyOntologyListItem.kdsModule());
    assertThat(searchResultEntry.availability()).isEqualTo(dummyOntologyListItem.availability());
    assertThat(searchResultEntry.context()).isEqualTo(dummyOntologyListItem.context().code());
    assertThat(searchResultEntry.termcode()).isEqualTo(dummyOntologyListItem.termcode());
  }

  @Test
  void testGetSearchResultEntryByHash_throwsOnNotFound() {
    doReturn(Optional.empty()).when(ontologyListItemEsRepository).findById(any(String.class));

    assertThrows(OntologyItemNotFoundException.class, () -> terminologyEsService.getSearchResultEntryByHash("id"));
  }

  @Test
  void testGetSearchResultEntryByCriterion() {
    String id = UUID.randomUUID().toString();
    OntologyListItemDocument dummyOntologyListItem = createDummyOntologyListItem(id);
    doReturn(Optional.of(dummyOntologyListItem)).when(ontologyListItemEsRepository).findById(any(String.class));

    var searchResultEntry = assertDoesNotThrow(() -> terminologyEsService.getSearchResultEntryByCriterion(createDummyCriterion(true)));

    assertThat(searchResultEntry).isNotNull();
    assertThat(searchResultEntry.id()).isEqualTo(id);
    assertThat(searchResultEntry.terminology()).isEqualTo(dummyOntologyListItem.terminology());
    assertThat(searchResultEntry.display().original()).isEqualTo(dummyOntologyListItem.display().original());
    assertThat(searchResultEntry.kdsModule()).isEqualTo(dummyOntologyListItem.kdsModule());
    assertThat(searchResultEntry.availability()).isEqualTo(dummyOntologyListItem.availability());
    assertThat(searchResultEntry.context()).isEqualTo(dummyOntologyListItem.context().code());
    assertThat(searchResultEntry.termcode()).isEqualTo(dummyOntologyListItem.termcode());
  }

  @Test
  void testGetSearchResultEntryByCriterion_throwsOnNotFound() {
    doReturn(Optional.empty()).when(ontologyListItemEsRepository).findById(any(String.class));

    assertThrows(OntologyItemNotFoundException.class, () -> terminologyEsService.getSearchResultEntryByCriterion(createDummyCriterion(true)));
  }

  @Test
  void testGetAvailableFilters() {
    var expectedTermFiltersList = createTermFilterList(filterFields);

    doReturn(searchHits).when(operations).search(any(NativeQuery.class), any());
    doReturn(elasticsearchAggregations).when(searchHits).getAggregations();
    // This fails when written as doReturn()...when(), but works in this order...so...
    when(elasticsearchAggregations.aggregationsAsMap().get(any(String.class)).aggregation().getAggregate().sterms()
        .buckets().array()).thenReturn(List.of(createStringTermsBucket()));

    var filters = terminologyEsService.getAvailableFilters();

    assertThat(filters.size()).isEqualTo(expectedTermFiltersList.size());
    assertThat(filters).containsAll(expectedTermFiltersList);
  }

  private static Stream<Arguments> generateArgumentsForTestPerformOntologySearchWithPaging() {
    var booleanList = List.of(true, false);
    var list = new ArrayList<Arguments>();

    for (boolean availability : booleanList) {
      list.add(Arguments.of(null, null, null, null, availability, 20, 0));
      list.add(Arguments.of(null, List.of("foo"), null, null, availability, 20, 0));
      list.add(Arguments.of(null, null, List.of("bar"), null, availability, 20, 0));
      list.add(Arguments.of(null, null, null, List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(null, List.of("foo"), List.of("bar"), null, availability, 20, 0));
      list.add(Arguments.of(null, List.of("foo"), null, List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(null, null, List.of("bar"), List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(null, List.of("foo"), List.of("bar"), List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), null, null, null, availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), List.of("foo"), null, null, availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), null, List.of("bar"), null, availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), null, null, List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), List.of("foo"), List.of("bar"), null, availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), List.of("foo"), null, List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), null, List.of("bar"), List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), List.of("foo"), List.of("bar"), List.of("baz"), availability, 20, 0));
    }

    return list.stream();
  }

  private static Stream<Arguments> generateArgumentsForTestPerformExactSearch() {
    var list = new ArrayList<Arguments>();

    list.add(Arguments.of(List.of("available-term-0", "available-term-1"), List.of(), "valid-terminology", "valid-context"));
    list.add(Arguments.of(List.of(), List.of("unavailable-term-0", "unavailable-term-1"), "valid-terminology", "valid-context"));
    list.add(Arguments.of(List.of("available-term-0", "available-term-1"), List.of("unavailable-term-0", "unavailable-term-1"), "valid-terminology", "valid-context"));
    list.add(Arguments.of(List.of(), List.of(), "valid-terminology", "valid-context"));
    list.add(Arguments.of(List.of(), List.of(), "", ""));

    return list.stream();
  }

  @ParameterizedTest
  @MethodSource("generateArgumentsForTestPerformOntologySearchWithPaging")
  void testPerformOntologySearchWithPaging(List<String> criteriaSets, List<String> context, List<String> kdsModule, List<String> terminology, Boolean availability, Integer pageSize, Integer page) {
    int totalHits = new Random().nextInt(100);
    SearchHits<OntologyListItemDocument> dummySearchHitsPage = createDummySearchHitsPageWithOntologyListItemDocuments(totalHits);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var searchResult = assertDoesNotThrow(
        () -> terminologyEsService.performOntologySearchWithPaging(
            "foobar", criteriaSets, context, kdsModule, terminology, availability, pageSize, page)
    );

    assertThat(searchResult.totalHits()).isEqualTo(totalHits);
    assertThat(searchResult.results().size()).isEqualTo(dummySearchHitsPage.getTotalHits());
    assertThat(searchResult.results()).containsExactlyInAnyOrderElementsOf(dummySearchHitsPage.getSearchHits().stream().map(sh -> EsSearchResultEntry.of(sh.getContent())).toList());
  }

  @ParameterizedTest
  @MethodSource("generateArgumentsForTestPerformOntologySearchWithPaging")
  void testPerformOntologySearchWithPagingEmptyKeyword(List<String> criteriaSets, List<String> context, List<String> kdsModule, List<String> terminology, Boolean availability, Integer pageSize, Integer page) {
    int totalHits = new Random().nextInt(100);
    SearchHits<OntologyListItemDocument> dummySearchHitsPage = createDummySearchHitsPageWithOntologyListItemDocuments(totalHits);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var searchResult = assertDoesNotThrow(
        () -> terminologyEsService.performOntologySearchWithPaging(
            "", criteriaSets, context, kdsModule, terminology, availability, pageSize, page)
    );

    assertThat(searchResult.totalHits()).isEqualTo(totalHits);
    assertThat(searchResult.results().size()).isEqualTo(dummySearchHitsPage.getTotalHits());
    assertThat(searchResult.results()).containsExactlyInAnyOrderElementsOf(dummySearchHitsPage.getSearchHits().stream().map(sh -> EsSearchResultEntry.of(sh.getContent())).toList());
  }

  @Test
  void testPerformOntologySearchWithRepoAndPaging_throwsOnInvalidPageSize() {
    assertThrows(IllegalArgumentException.class, () -> terminologyEsService.performOntologySearchWithPaging(
            "foobar", null, null, null, null, false, 0, 0)
    );
  }

  @ParameterizedTest
  @MethodSource("generateArgumentsForTestPerformExactSearch")
  void testPerformExactSearch(List<String> searchtermsFound, List<String> searchtermsNotFound, String terminology, String context) {
    List<String> searchterms =
        Stream.concat(searchtermsFound.stream(), searchtermsNotFound.stream()).collect(Collectors.toList());

    SearchHits<OntologyItemDocument> dummySearchHitsPage = createDummySearchHitsPageWithOntologyItemDocuments(searchtermsFound.size());
    var request = TerminologyBulkSearchRequest.builder()
        .terminology(terminology)
        .context(context)
        .searchterms(searchterms)
        .build();

    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));
    var searchResult = assertDoesNotThrow(
        () -> terminologyEsService.performExactSearch(request)
    );

    assertThat(searchResult.uiProfileId()).isNull();
    assertThat(searchResult.found().size()).isEqualTo(searchtermsFound.size());
    assertThat(searchResult.notFound().size()).isEqualTo(searchtermsNotFound.size());
    assertThat(searchResult.found()).containsExactlyInAnyOrderElementsOf(dummySearchHitsPage.getSearchHits().stream().map(sh -> EsSearchResultEntryExtended.of(sh.getContent())).toList());
    assertThat(searchResult.notFound()).containsExactlyInAnyOrderElementsOf(searchtermsNotFound);
  }

  @Test
  void testGetRelationEntryByHash_succeeds() {
    String id = UUID.randomUUID().toString();
    var dummyOntologyItem = createDummyOntologyItem(id);
    doReturn(Optional.of(dummyOntologyItem)).when(ontologyItemEsRepository).findById(any(String.class));

    var relationEntry = assertDoesNotThrow(() -> terminologyEsService.getRelationEntryByHash(id));
    assertThat(relationEntry).isNotNull();
    assertThat(relationEntry.relatedTerms()).isEqualTo(dummyOntologyItem.relatedTerms().stream().map(RelativeEntry::of).collect(Collectors.toList()));
    assertThat(relationEntry.children()).isEqualTo(dummyOntologyItem.children().stream().map(RelativeEntry::of).collect(Collectors.toList()));
    assertThat(relationEntry.parents()).isEqualTo(dummyOntologyItem.parents().stream().map(RelativeEntry::of).collect(Collectors.toList()));
    assertThat(relationEntry.display()).isEqualTo(DisplayEntry.of(dummyOntologyItem.display()));
  }

  @Test
  void testGetRelationEntryByHash_throwsOnNotFound() {
    doReturn(Optional.empty()).when(ontologyItemEsRepository).findById(any(String.class));

    assertThrows(OntologyItemNotFoundException.class, () -> terminologyEsService.getRelationEntryByHash("id"));
  }

  @Test
  void testCreateContextualizedTermcodeHash_succeeds() {
    var expectedUuid = "f0bd4e88-6f61-36c5-9551-87976858971b";
    var criterion = createDummyCriterion(true);

    var result = assertDoesNotThrow(() -> TerminologyEsService.createContextualizedTermcodeHash(criterion));

    assertThat(result).isNotNull();
    assertEquals(expectedUuid, result);
  }

  @Test
  void testCreateContextualizedTermcodeHash_emptyVersion() {
    var expectedUuid = "a6597204-1552-35e9-ba23-d734998f4039";
    var criterion = createDummyCriterion(false);

    var result = assertDoesNotThrow(() -> TerminologyEsService.createContextualizedTermcodeHash(criterion));

    assertThat(result).isNotNull();
    assertEquals(expectedUuid, result);
  }

  @Test
  void testCreateContextualizedTermcodeHash_equalInputs() {
    var criterion1 = createDummyCriterion(true);
    var criterion2 = createDummyCriterion(true);

    var hash1 = TerminologyEsService.createContextualizedTermcodeHash(criterion1);
    var hash2 = TerminologyEsService.createContextualizedTermcodeHash(criterion2);

    assertEquals(hash1, hash2);
  }

  @Test
  void testCreateContextualizedTermcodeHash_unequalInputs() {
    var criterion1 = createDummyCriterion(true);
    var criterion2 = createDummyCriterion(false);

    var hash1 = TerminologyEsService.createContextualizedTermcodeHash(criterion1);
    var hash2 = TerminologyEsService.createContextualizedTermcodeHash(criterion2);

    assertNotEquals(hash1, hash2);
  }

  private OntologyListItemDocument createDummyOntologyListItem(String id) {
    TermCode termCode = createDummyTermCode();

    return OntologyListItemDocument.builder()
        .id(id)
        .display(createDummyDisplay())
        .availability(1)
        .context(termCode)
        .terminology("Some Terminology")
        .termcode("Some Termcode")
        .kdsModule("Some KDS Module")
        .selectable(true)
        .build();
  }

  private Display createDummyDisplay() {
    return Display.builder()
        .original("Some Name")
        .deDe("Some German Name")
        .enUs("Some English Name")
        .build();
  }

  private OntologyItemDocument createDummyOntologyItem(String id, String termcode) {
    TermCode termCode = createDummyTermCode();
    Collection<Relative> parents = List.of(createDummyRelative(), createDummyRelative());
    Collection<Relative> children = List.of(createDummyRelative(), createDummyRelative(), createDummyRelative(), createDummyRelative());
    Collection<Relative> relatedTerms = List.of(createDummyRelative(), createDummyRelative());

    return OntologyItemDocument.builder()
        .id(id)
        .display(createDummyDisplay())
        .availability(1)
        .context(termCode)
        .terminology("Some Terminology")
        .termcode(termcode)
        .kdsModule("Some KDS Module")
        .selectable(true)
        .parents(parents)
        .children(children)
        .relatedTerms(relatedTerms)
        .build();
  }

  private OntologyItemDocument createDummyOntologyItem(String id) {
    return createDummyOntologyItem(id, "Some Termcode");
  }

  private Relative createDummyRelative() {
    return Relative.builder()
        .contextualizedTermcodeHash(UUID.randomUUID().toString())
        .display(createDummyDisplay())
        .build();
  }

  private TermCode createDummyTermCode(boolean includeVersion) {
    return TermCode.builder()
        .code("abc-123")
        .version(includeVersion ? "2024" : null)
        .system("some-system")
        .display("this is a dummy termcode")
        .build();
  }

  private TermCode createDummyTermCode() {
    return createDummyTermCode(true);
  }

  private Criterion createDummyCriterion(boolean includeVersion) {
    return Criterion.builder()
        .context(createDummyTermCode(includeVersion))
        .termCodes(List.of(createDummyTermCode(includeVersion)))
        .build();
  }

  private StringTermsBucket createStringTermsBucket() {
    StringTermsBucket.Builder builder = new StringTermsBucket.Builder();
    for (String filter : filterFields) {
      builder.key(filter);
    }
    builder.docCount(filterFields.length);
    return builder.build();
  }

  private List<TermFilter> createTermFilterList(String[] values) {
    var termFilters = new ArrayList<TermFilter>();
    for (String term : values) {
      termFilters.add(
          TermFilter.builder()
              .name(term)
              .type("selectable-concept")
              .values(List.of(TermFilterValue.builder().label("baz").count(values.length).build()))
              .build()
      );
    }
    // Hardcode availability filter. It is not yet decided if and how this will be available anyways. TODO!
    termFilters.add(TermFilter.builder()
        .type("boolean")
        .name("availability")
        .values(List.of())
        .build());
    return termFilters;
  }

  private SearchHits<OntologyListItemDocument> createDummySearchHitsPageWithOntologyListItemDocuments(int totalHits) {
    var searchHitsList = new ArrayList<SearchHit<OntologyListItemDocument>>();

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
              createDummyOntologyListItem(UUID.randomUUID().toString())
          )
      );
    }
    return new SearchHitsImpl<>(totalHits, TotalHitsRelation.OFF, 10.0F, null, null, null, searchHitsList, null, null, null);
  }

  private SearchHits<OntologyItemDocument> createDummySearchHitsPageWithOntologyItemDocuments(int totalHits) {
    var searchHitsList = new ArrayList<SearchHit<OntologyItemDocument>>();

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
              createDummyOntologyItem(UUID.randomUUID().toString(), String.format("available-term-%d", i))
          )
      );
    }
    return new SearchHitsImpl<>(totalHits, TotalHitsRelation.OFF, 10.0F, null, null, null, searchHitsList, null, null, null);
  }
}
