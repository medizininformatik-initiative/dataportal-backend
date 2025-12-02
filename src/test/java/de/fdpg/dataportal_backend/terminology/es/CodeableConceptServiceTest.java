package de.fdpg.dataportal_backend.terminology.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fdpg.dataportal_backend.common.api.TermCode;
import de.fdpg.dataportal_backend.dse.api.LocalizedValue;
import de.fdpg.dataportal_backend.terminology.api.CodeableConceptBulkSearchRequest;
import de.fdpg.dataportal_backend.terminology.api.CodeableConceptEntry;
import de.fdpg.dataportal_backend.terminology.es.model.CodeableConceptDocument;
import de.fdpg.dataportal_backend.terminology.es.model.Display;
import de.fdpg.dataportal_backend.terminology.es.repository.CodeableConceptEsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CodeableConceptServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Mock
  ElasticsearchOperations operations;
  @Mock
  CodeableConceptEsRepository repository;
  private CodeableConceptService codeableConceptService;

  private static Stream<Arguments> generateArgumentsForTestPerformExactSearch() {
    var list = new ArrayList<Arguments>();

    list.add(Arguments.of(List.of("available-term-0", "available-term-1"), List.of(), "valid-valueset"));
    list.add(Arguments.of(List.of(), List.of("unavailable-term-0", "unavailable-term-1"), "valid-valueset"));
    list.add(Arguments.of(List.of("available-term-0", "available-term-1"), List.of("unavailable-term-0", "unavailable-term-1"), "valid-valueset"));
    list.add(Arguments.of(List.of(), List.of(), "valid-valueset"));
    list.add(Arguments.of(List.of(), List.of(), ""));

    return list.stream();
  }

  private CodeableConceptService createCodeableConceptService() {
    return new CodeableConceptService(operations, repository);
  }

  @BeforeEach
  void setUp() {
    Mockito.reset(operations, repository);
    codeableConceptService = createCodeableConceptService();
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithoutValueSetFilter() {
    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", List.of(), 20, 0));

    assertNotNull(result);
    assertEquals(dummySearchHitsPage.getTotalHits(), result.getTotalHits());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().termCode(), result.getResults().get(0).termCode());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().display().original(), result.getResults().get(0).display().original());
    assertTrue(result.getResults().get(0).display().translations().containsAll(
            List.of(
                LocalizedValue.builder()
                    .value(dummySearchHitsPage.getSearchHits().get(0).getContent().display().deDe())
                    .language("de-DE")
                    .build(),
                LocalizedValue.builder()
                    .value(dummySearchHitsPage.getSearchHits().get(0).getContent().display().enUs())
                    .language("en-US")
                    .build())
        )
    );
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithNullValueSetFilter() {
    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", null, 20, 0));

    assertNotNull(result);
    assertEquals(dummySearchHitsPage.getTotalHits(), result.getTotalHits());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().termCode(), result.getResults().get(0).termCode());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().display().original(), result.getResults().get(0).display().original());
    assertTrue(result.getResults().get(0).display().translations().containsAll(
            List.of(
                LocalizedValue.builder()
                    .value(dummySearchHitsPage.getSearchHits().get(0).getContent().display().deDe())
                    .language("de-DE")
                    .build(),
                LocalizedValue.builder()
                    .value(dummySearchHitsPage.getSearchHits().get(0).getContent().display().enUs())
                    .language("en-US")
                    .build())
        )
    );
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithValueSetFilter() {
    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", List.of("bar"), 20, 0));

    assertNotNull(result);
    assertEquals(dummySearchHitsPage.getTotalHits(), result.getTotalHits());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().termCode(), result.getResults().get(0).termCode());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().display().original(), result.getResults().get(0).display().original());
    assertTrue(result.getResults().get(0).display().translations().containsAll(
            List.of(
                LocalizedValue.builder()
                    .value(dummySearchHitsPage.getSearchHits().get(0).getContent().display().deDe())
                    .language("de-DE")
                    .build(),
                LocalizedValue.builder()
                    .value(dummySearchHitsPage.getSearchHits().get(0).getContent().display().enUs())
                    .language("en-US")
                    .build())
        )
    );
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithEmptyResult() {
    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(0);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", List.of(), 20, 0));

    assertNotNull(result);
    assertThat(result.getTotalHits()).isZero();
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithEmptyKeyword() {
    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("", List.of(), 20, 0));

    assertNotNull(result);
    assertThat(result.getTotalHits()).isNotZero();
  }

  @ParameterizedTest
  @MethodSource("generateArgumentsForTestPerformExactSearch")
  void testPerformExactSearch(List<String> searchtermsFound, List<String> searchtermsNotFound, String valueSet) {
    List<String> searchterms =
        Stream.concat(searchtermsFound.stream(), searchtermsNotFound.stream()).collect(Collectors.toList());

    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(searchtermsFound.size());
    var request = CodeableConceptBulkSearchRequest.builder()
        .valueSet(valueSet)
        .searchterms(searchterms)
        .build();

    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));
    var searchResult = assertDoesNotThrow(
        () -> codeableConceptService.performExactSearch(request)
    );

    assertThat(searchResult.found().size()).isEqualTo(searchtermsFound.size());
    assertThat(searchResult.notFound().size()).isEqualTo(searchtermsNotFound.size());
    assertThat(searchResult.found()).containsExactlyInAnyOrderElementsOf(dummySearchHitsPage.getSearchHits().stream().map(sh -> CodeableConceptEntry.of(sh.getContent())).toList());
    assertThat(searchResult.notFound()).containsExactlyInAnyOrderElementsOf(searchtermsNotFound);
  }

  @Test
  void testGetSearchResultsEntryByIds_succeeds() {
    CodeableConceptDocument dummyCodeableConceptDocument = createDummyCodeableConceptDocument("1");
    doReturn(List.of(dummyCodeableConceptDocument)).when(repository).findAllById(any());

    var result = assertDoesNotThrow(() -> codeableConceptService.getSearchResultsEntryByIds(List.of("1")));

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(dummyCodeableConceptDocument.termCode(), result.get(0).termCode());
    assertEquals(dummyCodeableConceptDocument.display().original(), result.get(0).display().original());
    assertTrue(result.get(0).display().translations().containsAll(
        List.of(
            LocalizedValue.builder()
                .value(dummyCodeableConceptDocument.display().deDe())
                .language("de-DE")
                .build(),
            LocalizedValue.builder()
                .value(dummyCodeableConceptDocument.display().enUs())
                .language("en-US")
                .build()
        )
    ));
  }

  @Test
  void testGetSearchResultsEntryByIds_emptyOnNotFound() {
    doReturn(List.of()).when(repository).findAllById(anyList());

    var result = assertDoesNotThrow(() -> codeableConceptService.getSearchResultsEntryByIds(List.of("foo")));
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetSearchResultsEntryByTermcode_succeeds() {
    CodeableConceptDocument dummyCodeableConceptDocument = createDummyCodeableConceptDocument("1");
    doReturn(List.of(dummyCodeableConceptDocument)).when(repository).findAllById(any());

    var result = assertDoesNotThrow(() -> codeableConceptService.getSearchResultEntryByTermCode(createDummyTermcode()));

    assertNotNull(result);
    assertEquals(dummyCodeableConceptDocument.termCode(), result.termCode());
    assertEquals(dummyCodeableConceptDocument.display().original(), result.display().original());
    assertTrue(result.display().translations().containsAll(
        List.of(
            LocalizedValue.builder()
                .value(dummyCodeableConceptDocument.display().deDe())
                .language("de-DE")
                .build(),
            LocalizedValue.builder()
                .value(dummyCodeableConceptDocument.display().enUs())
                .language("en-US")
                .build()
        )
    ));
  }

  @Test
  void testGetSearchResultsEntryByTermcode_nullOnException() {
    doReturn(List.of()).when(repository).findAllById(anyList());

    var result = assertDoesNotThrow(() -> codeableConceptService.getSearchResultEntryByTermCode(createDummyTermcode()));
    assertNull(result);
  }

  private SearchHits<CodeableConceptDocument> createDummySearchHitsPage(int totalHits) {
    var searchHitsList = new ArrayList<SearchHit<CodeableConceptDocument>>();

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
              createDummyCodeableConceptDocument(UUID.randomUUID().toString(), String.format("available-term-%d", i))
          )
      );
    }
    return new SearchHitsImpl<>(totalHits, TotalHitsRelation.OFF, 10.0F, null, null, null, searchHitsList, null, null, null);
  }

  private CodeableConceptDocument createDummyCodeableConceptDocument(String id, String termCodeCode) {
    return CodeableConceptDocument.builder()
        .id(id)
        .termCode(createDummyTermcode(termCodeCode))
        .display(createDummyDisplay())
        .valueSets(List.of())
        .build();
  }

  private CodeableConceptDocument createDummyCodeableConceptDocument(String id) {
    return createDummyCodeableConceptDocument(id, createDummyTermcode().code());
  }

  private Display createDummyDisplay() {
    return Display.builder()
        .original("code-1")
        .deDe("Code 1")
        .enUs("Code One")
        .build();
  }

  private TermCode createDummyTermcode(String code) {
    return TermCode.builder()
        .code(code)
        .display("Code 1")
        .system("http://system1")
        .version("9000")
        .build();
  }

  private TermCode createDummyTermcode() {
    return createDummyTermcode("code-1");
  }
}
