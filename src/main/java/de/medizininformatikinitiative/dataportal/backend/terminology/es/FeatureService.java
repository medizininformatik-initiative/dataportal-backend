package de.medizininformatikinitiative.dataportal.backend.terminology.es;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.FeatureEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.FeatureSearchResult;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.FeatureDocument;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.FeatureEsRepository;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.FeatureNotFoundException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@ConditionalOnExpression("${app.elastic.enabled}")
public class FeatureService {
  public static final String FIELD_DISPLAY_DE = "display.de";
  public static final String FIELD_DISPLAY_DE_WITH_BOOST = "display.de^2";
  public static final String FIELD_DISPLAY_DE_NGRAM = "display.de.ngram";
  public static final String FIELD_DISPLAY_EN = "display.en";
  public static final String FIELD_DISPLAY_EN_WITH_BOOST = "display.en^2";
  public static final String FIELD_DISPLAY_EN_NGRAM = "display.en.ngram";
  public static final String FIELD_DISPLAY_ORIGINAL = "display.original";
  public static final String FIELD_DISPLAY_ORIGINAL_WITH_BOOST = "display.original^2";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_NAME_WITH_BOOST = "name^3";
  public static final String FIELD_NAME_NGRAM = "name.ngram";
  public static final String FIELD_FIELDS_DE = "fields.de";
  public static final String FIELD_FIELDS_DE_NGRAM = "fields.de.ngram";
  public static final String FIELD_FIELDS_EN = "fields.en";
  public static final String FIELD_FIELDS_EN_NGRAM = "fields.en.ngram";
  public static final String FIELD_FIELDS_ORIGINAL = "fields.original";
  public static final String FIELD_FIELDS_ORIGINAL_NGRAM = "fields.original.ngram";
  public static final String FIELD_TERMCODE_CODE_WITH_BOOST = "termcode.code^3";
  public static final String FILTER_KEY_MODULE = "module";
  public static final String FILTER_KEY_CATEGORIES = "categories";

  private ElasticsearchOperations operations;

  private FeatureEsRepository repo;

  @Autowired
  public FeatureService(ElasticsearchOperations operations, FeatureEsRepository repo) {
    this.operations = operations;
    this.repo = repo;
  }

  public FeatureSearchResult performFeatureSearchWithRepoAndPaging(String keyword,
                                                                    @Nullable List<String> modules,
                                                                    @Nullable List<String> categories,
                                                                    @Nullable int pageSize,
                                                                    @Nullable int page) {

    List<Pair<String, List<String>>> filterList = new ArrayList<>();
    if (!CollectionUtils.isEmpty(modules)) {
      filterList.add(Pair.of(FILTER_KEY_MODULE, modules));
    }
    if (!CollectionUtils.isEmpty(categories)) {
      filterList.add(Pair.of(FILTER_KEY_CATEGORIES, categories));
    }

    var searchHitPage = findByNameOrDisplay(keyword, filterList, PageRequest.of(page, pageSize));
    List<FeatureEntry> featureEntries = new ArrayList<>();

    searchHitPage.getSearchHits().forEach(hit -> featureEntries.add(FeatureEntry.of(hit.getContent())));
    return FeatureSearchResult.builder()
        .totalHits(searchHitPage.getTotalHits())
        .results(featureEntries)
        .build();
  }

  public FeatureEntry getSearchResultEntryById(String id) {
    return repo.findById(id).map(FeatureEntry::of).orElseThrow(FeatureNotFoundException::new);
  }

  private SearchHits<FeatureDocument> findByNameOrDisplay(String keyword,
                                                           List<Pair<String, List<String>>> filterList,
                                                           PageRequest pageRequest) {
    List<Query> filterTerms = new ArrayList<>();

    if (!filterList.isEmpty()) {
      filterList.forEach(f -> {
        var fieldValues = f.getSecond().stream().map(FieldValue::of).toList();
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
      var translatedDisplayMissing = new BoolQuery.Builder()
          .mustNot(
              new ExistsQuery.Builder().field(FIELD_DISPLAY_DE).build()._toQuery(),
              new ExistsQuery.Builder().field(FIELD_DISPLAY_EN).build()._toQuery()
          )
          .build();

      var translatedMatch = new BoolQuery.Builder()
          .must(new MultiMatchQuery.Builder()
              .query(keyword)
              .fields(List.of(
                  FIELD_DISPLAY_DE_WITH_BOOST,
                  FIELD_DISPLAY_EN_WITH_BOOST,
                  FIELD_NAME_WITH_BOOST,
                  FIELD_FIELDS_DE,
                  FIELD_FIELDS_EN,
                  FIELD_FIELDS_DE_NGRAM,
                  FIELD_FIELDS_EN_NGRAM,
                  FIELD_DISPLAY_DE_NGRAM,
                  FIELD_DISPLAY_EN_NGRAM,
                  FIELD_NAME_NGRAM
              ))
              .build()._toQuery())
          .mustNot(translatedDisplayMissing._toQuery())
          .build();

      var originalMatch = new BoolQuery.Builder()
          .must(
              new ExistsQuery.Builder().field(FIELD_DISPLAY_ORIGINAL).build()._toQuery(),
              new MultiMatchQuery.Builder()
                  .query(keyword)
                  .fields(List.of(
                      FIELD_DISPLAY_ORIGINAL_WITH_BOOST,
                      FIELD_TERMCODE_CODE_WITH_BOOST,
                      FIELD_FIELDS_ORIGINAL,
                      FIELD_FIELDS_ORIGINAL_NGRAM,
                      FIELD_NAME
                  ))
                  .build()._toQuery()
          )
          .build();

      boolQuery = new BoolQuery.Builder()
          .should(translatedMatch._toQuery(), originalMatch._toQuery())
          .minimumShouldMatch("1")
          .filter(filterTerms.isEmpty() ? List.of() : filterTerms)
          .build();
    }

    var query = new NativeQueryBuilder()
        .withQuery(boolQuery._toQuery())
        .withPageable(pageRequest)
        .build();

    log.info(Objects.requireNonNull(query.getQuery()).toString());

    return operations.search(query, FeatureDocument.class);
  }
}
