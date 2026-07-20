package de.medizininformatikinitiative.dataportal.backend.terminology.es;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileSearchEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileSearchResult;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileDocument;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.TermFilter;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.TermFilterValue;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.ProfileEsRepository;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.ProfileNotFoundException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@ConditionalOnExpression("${app.elastic.enabled}")
public class ProfileService {
  public static final String FIELD_DISPLAY_DE = "display.localization.de-DE";
  public static final String FIELD_DISPLAY_DE_WITH_BOOST = "display.localization.de-DE^2";
  public static final String FIELD_DISPLAY_DE_NGRAM = "display.localization.de-DE.ngram";
  public static final String FIELD_DISPLAY_EN = "display.localization.en-US";
  public static final String FIELD_DISPLAY_EN_WITH_BOOST = "display.localization.en-US^2";
  public static final String FIELD_DISPLAY_EN_NGRAM = "display.localization.en-US.ngram";
  public static final String FIELD_DISPLAY_ORIGINAL = "display.original";
  public static final String FIELD_DISPLAY_ORIGINAL_WITH_BOOST = "display.original^2";
  public static final String FIELD_NAME_WITH_BOOST = "name^3";
  public static final String FIELD_NAME_NGRAM = "name.ngram";
  public static final String FIELD_FIELDS_DE = "fields.display.localization.de-DE";
  public static final String FIELD_FIELDS_DE_NGRAM = "fields.display.localization.de-DE.ngram";
  public static final String FIELD_FIELDS_EN = "fields.display.localization.en-US";
  public static final String FIELD_FIELDS_EN_NGRAM = "fields.display.localization.en-US.ngram";
  public static final String FIELD_FIELDS_ORIGINAL = "fields.display.original";
  public static final String FIELD_FIELDS_ORIGINAL_NGRAM = "fields.display.original.ngram";
  public static final String FILTER_KEY_MODULE = "module.display.original";
  public static final String FILTER_KEY_CATEGORIES = "categories.display.original";
  public static final String FILTER_NAME_MODULE = "module";
  public static final String FILTER_NAME_CATEGORY = "category";

  private ElasticsearchOperations operations;

  private ProfileEsRepository repo;

  @Autowired
  public ProfileService(ElasticsearchOperations operations, ProfileEsRepository repo) {
    this.operations = operations;
    this.repo = repo;
  }

  public ProfileSearchResult performProfileSearchWithRepoAndPaging(String keyword,
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
    List<ProfileSearchEntry> profileEntries = new ArrayList<>();

    searchHitPage.getSearchHits().forEach(hit -> profileEntries.add(ProfileSearchEntry.of(hit.getContent())));
    return ProfileSearchResult.builder()
        .totalHits(searchHitPage.getTotalHits())
        .results(profileEntries)
        .build();
  }

  public ProfileEntry getProfileListDetailsById(String id) {
    return repo.findById(id).map(ProfileEntry::of).orElseThrow(ProfileNotFoundException::new);
  }

  public List<TermFilter> getAvailableFilters() {
    return List.of(
        getFilter(FILTER_NAME_MODULE, FILTER_KEY_MODULE),
        getFilter(FILTER_NAME_CATEGORY, FILTER_KEY_CATEGORIES)
    );
  }

  private TermFilter getFilter(String name, String field) {
    var aggregationQuery = NativeQuery.builder()
        .withAggregation(field, Aggregation.of(a -> a
            .terms(ta -> ta.field(field).size(50))))
        .withMaxResults(0)
        .build();

    SearchHits<ProfileDocument> searchHits = operations.search(aggregationQuery, ProfileDocument.class);
    ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();
    assert aggregations != null;
    List<StringTermsBucket> buckets = aggregations.aggregationsAsMap().get(field).aggregation().getAggregate().sterms().buckets().array();

    List<TermFilterValue> values = new ArrayList<>();
    buckets.forEach(b -> {
      if (!b.key().stringValue().isEmpty()) {
        values.add(TermFilterValue.builder()
            .label(b.key().stringValue())
            .count(b.docCount())
            .build());
      }
    });

    return TermFilter.builder()
        .name(name)
        .type("selectable-concept")
        .values(values)
        .build();
  }

  private SearchHits<ProfileDocument> findByNameOrDisplay(String keyword,
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
                      FIELD_NAME_WITH_BOOST,
                      FIELD_FIELDS_ORIGINAL,
                      FIELD_FIELDS_ORIGINAL_NGRAM,
                      FIELD_NAME_NGRAM
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

    return operations.search(query, ProfileDocument.class);
  }
}
