package de.numcodex.feasibility_gui_backend.terminology.es;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.terminology.api.*;
import de.numcodex.feasibility_gui_backend.terminology.es.model.CodeableConceptDocument;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.CodeableConceptEsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnExpression("${app.elastic.enabled}")
public class CodeableConceptService {
  public static final String FIELD_NAME_DISPLAY_DE = "display.de";
  public static final String FIELD_NAME_DISPLAY_EN = "display.en";
  public static final String FIELD_NAME_DISPLAY_ORIGINAL_WITH_BOOST = "display.original^0.5";
  public static final String FIELD_NAME_TERMCODE_WITH_BOOST = "termcode.code^2";
  public static final String FIELD_NAME_TERMCODE_KEYWORD = "termcode.code.keyword";
  public static final String FILTER_KEY_VALUE_SETS = "value_sets";
  private static final UUID NAMESPACE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private ElasticsearchOperations operations;

  private CodeableConceptEsRepository repo;

  @Autowired
  public CodeableConceptService(ElasticsearchOperations operations, CodeableConceptEsRepository repo) {
    this.operations = operations;
    this.repo = repo;
  }

  public CcSearchResult performCodeableConceptSearchWithRepoAndPaging(String keyword,
                                                               @Nullable List<String> valueSets,
                                                               @Nullable int pageSize,
                                                               @Nullable int page) {

    List<Pair<String, List<String>>> filterList = new ArrayList<>();
    if (!CollectionUtils.isEmpty(valueSets)) {
      filterList.add(Pair.of(FILTER_KEY_VALUE_SETS, valueSets));
    }

    var searchHitPage = findByCodeOrDisplay(keyword, filterList, PageRequest.of(page, pageSize));
    List<CodeableConceptEntry> codeableConceptEntries = new ArrayList<>();

    searchHitPage.getSearchHits().forEach(hit -> codeableConceptEntries.add(CodeableConceptEntry.of(hit.getContent())));
    return CcSearchResult.builder()
        .totalHits(searchHitPage.getTotalHits())
        .results(codeableConceptEntries)
        .build();
  }

  public CodeableConceptBulkSearchResult performExactSearch(CodeableConceptBulkSearchRequest request) {
    List<CodeableConceptEntry> results = new ArrayList<>();
    List<String> notFound = new ArrayList<>(request.searchterms());

    SearchHits<CodeableConceptDocument> searchHitPage = findExactMatchesByBulkSearchRequest(request);
    searchHitPage.getSearchHits().forEach(hit -> {
      CodeableConceptDocument content = hit.getContent();
      results.add(CodeableConceptEntry.of(content));
      notFound.remove(content.termCode().code());
    });

    return CodeableConceptBulkSearchResult.builder()
        .found(results)
        .notFound(notFound)
        .build();
  }

  public List<CodeableConceptEntry> getSearchResultsEntryByIds(List<String> ids) {
    var documents = repo.findAllById(ids);
    var codeableConceptEntries = new ArrayList<CodeableConceptEntry>();
    documents.forEach(d -> codeableConceptEntries.add(CodeableConceptEntry.of(d)));
    return codeableConceptEntries;
  }

  public CodeableConceptEntry getSearchResultEntryByTermCode(TermCode termCode) {
    String termCodeConcat = MessageFormat.format("{0}{1}",
        termCode.code(),
        termCode.system()
    );
    try {
      return getSearchResultsEntryByIds(List.of(TerminologyEsService.createUuidV3(NAMESPACE_UUID, termCodeConcat).toString())).get(0);
    } catch (IndexOutOfBoundsException e) {
      return null;
    }
  }

  private SearchHits<CodeableConceptDocument> findByCodeOrDisplay(String keyword,
                                                                  List<Pair<String,List<String>>> filterList,
                                                                  PageRequest pageRequest) {
    List<Query> filterTerms = new ArrayList<>();

    if (!filterList.isEmpty()) {
      var fieldValues = new ArrayList<FieldValue>();
      filterList.forEach(f -> {
        f.getSecond().forEach(s -> fieldValues.add(new FieldValue.Builder().stringValue(s).build()));
        filterTerms.add(new TermsQuery.Builder()
            .field(f.getFirst())
            .terms(new TermsQueryField.Builder().value(fieldValues).build())
            .build()._toQuery());
      });
    }

    BoolQuery boolQuery;

    if (keyword.isEmpty()) {
      boolQuery = new BoolQuery.Builder()
          .filter(filterTerms.isEmpty() ? List.of() : filterTerms)
          .build();

    } else {
      var multiMatchQuery = new MultiMatchQuery.Builder()
          .query(keyword)
          .fields(List.of(FIELD_NAME_DISPLAY_DE, FIELD_NAME_DISPLAY_EN, FIELD_NAME_TERMCODE_WITH_BOOST, FIELD_NAME_DISPLAY_ORIGINAL_WITH_BOOST))
          .build();

      boolQuery = new BoolQuery.Builder()
          .must(List.of(multiMatchQuery._toQuery()))
          .filter(filterTerms.isEmpty() ? List.of() : filterTerms)
          .build();

    }

    var query = new NativeQueryBuilder()
        .withQuery(boolQuery._toQuery())
        .withPageable(pageRequest)
        .build();

    log.info(Objects.requireNonNull(query.getQuery()).toString());

    return operations.search(query, CodeableConceptDocument.class);
  }

  private SearchHits<CodeableConceptDocument> findExactMatchesByBulkSearchRequest(CodeableConceptBulkSearchRequest request) {
    var boolQueryBuilder = new BoolQuery.Builder();

    boolQueryBuilder
        .filter(f -> f.term(t -> t.field(FILTER_KEY_VALUE_SETS).value(request.valueSet())));

    for (String code : request.searchterms()) {
      boolQueryBuilder.should(s -> s.term(t -> t.field(FIELD_NAME_TERMCODE_KEYWORD).value(code)));
    }

    boolQueryBuilder.minimumShouldMatch("1");

    var innerQuery = Query.of(q -> q.bool(boolQueryBuilder.build()));

    var finalQuery = new NativeQueryBuilder()
        .withQuery(innerQuery)
        .build();

    log.info(finalQuery.getQuery().toString());
    return operations.search(finalQuery, CodeableConceptDocument.class);
  }

}
