package de.numcodex.feasibility_gui_backend.terminology.es;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.terminology.api.CcSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.api.CodeableConceptEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.model.CodeableConceptDocument;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.CodeableConceptEsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.*;

@Service
@Slf4j
@ConditionalOnExpression("${app.elastic.enabled}")
public class CodeableConceptService {
  public static final String FIELD_NAME_DISPLAY_DE = "display.de";
  public static final String FIELD_NAME_DISPLAY_EN = "display.en";
  public static final String FIELD_NAME_DISPLAY_ORIGINAL_WITH_BOOST = "display.original^0.5";
  public static final String FIELD_NAME_TERMCODE_WITH_BOOST = "termcode.code^2";
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
      filterList.add(Pair.of("value_sets", valueSets));
    }

    var searchHitPage = findByCodeOrDisplay(keyword, filterList, PageRequest.of(page, pageSize));
    List<CodeableConceptEntry> codeableConceptEntries = new ArrayList<>();

    searchHitPage.getSearchHits().forEach(hit -> codeableConceptEntries.add(CodeableConceptEntry.of(hit.getContent())));
    return CcSearchResult.builder()
        .totalHits(searchHitPage.getTotalHits())
        .results(codeableConceptEntries)
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

  public List<String> availableCodesInValueSets(List<String> codes, List<String> valueSetUrls) {
    if (codes == null || codes.isEmpty() || valueSetUrls == null || valueSetUrls.isEmpty()) {
      throw new IllegalArgumentException();
    }
    var codesFound = new ArrayList<String>();

    var termsQueryField = new TermsQueryField.Builder()
        .value(valueSetUrls.stream().map(FieldValue::of).distinct().toList())
        .build();

    var termsQuery = new TermsQuery.Builder()
        .field("value_sets")
        .terms(termsQueryField)
        .build();

    var termsInclude = new TermsInclude.Builder()
        .terms(codes)
        .build();

    var aggregationQuery = NativeQuery.builder()
        .withQuery(termsQuery._toQuery())
        .withAggregation("existing_codes",
            Aggregation.of(a ->
                a.terms(ta ->
                    ta.field("termcode.code.keyword")
                        .include(termsInclude)
                        .size(1000)
                )))
        .withMaxResults(0)
        .build();

    var searchHits = operations.search(aggregationQuery, CodeableConceptDocument.class);
    var aggregations = (ElasticsearchAggregations) searchHits.getAggregations();
    assert aggregations != null;
    List<StringTermsBucket> buckets = aggregations.aggregationsAsMap().get("existing_codes").aggregation().getAggregate().sterms().buckets().array();
    buckets.forEach(bucket -> {
      if (bucket.docCount() > 0 ) {
        codesFound.add(bucket.key().stringValue());
      }
    });

    return codesFound;
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

}
