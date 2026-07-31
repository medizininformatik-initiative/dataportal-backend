package de.medizininformatikinitiative.dataportal.backend.terminology.es;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.dse.api.LocalizedValue;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileFilter;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileFilterValue;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileSearchEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileSearchResult;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.ProfileDocument;
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
import java.util.stream.Stream;

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
  public static final String FILTER_KEY_MODULE = "module.original";
  public static final String FILTER_KEY_CATEGORIES = "categories.original";
  public static final String FILTER_KEY_RESOURCE_TYPE = "resourceType.original";
  public static final String FIELD_BASE_MODULE = "module";
  public static final String FIELD_BASE_CATEGORIES = "categories";
  public static final String FIELD_BASE_RESOURCE_TYPE = "resourceType";
  public static final String FIELD_SELECTABLE = "selectable";
  public static final String FILTER_NAME_MODULE = "module";
  public static final String FILTER_NAME_CATEGORY = "category";
  public static final String FILTER_NAME_RESOURCE_TYPE = "resourceType";

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
                                                                    @Nullable List<String> resourceTypes,
                                                                    @Nullable int pageSize,
                                                                    @Nullable int page) {

    List<Pair<String, List<String>>> filterList = new ArrayList<>();
    if (!CollectionUtils.isEmpty(modules)) {
      filterList.add(Pair.of(FILTER_KEY_MODULE, modules));
    }
    if (!CollectionUtils.isEmpty(categories)) {
      filterList.add(Pair.of(FILTER_KEY_CATEGORIES, categories));
    }
    if (!CollectionUtils.isEmpty(resourceTypes)) {
      filterList.add(Pair.of(FILTER_KEY_RESOURCE_TYPE, resourceTypes));
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

  public List<ProfileFilter> getAvailableFilters() {
    return List.of(
        getFilter(FILTER_NAME_MODULE, FILTER_KEY_MODULE, FIELD_BASE_MODULE, null, null, null, null),
        getFilter(FILTER_NAME_CATEGORY, FILTER_KEY_CATEGORIES, FIELD_BASE_CATEGORIES, null, null, null, null),
        getFilter(FILTER_NAME_RESOURCE_TYPE, FILTER_KEY_RESOURCE_TYPE, FIELD_BASE_RESOURCE_TYPE, null, null, null, null)
    );
  }

  public List<ProfileFilter> getAvailableFilters(String targetFilter,
                                                  @Nullable String searchTerm,
                                                  @Nullable List<String> modules,
                                                  @Nullable List<String> categories,
                                                  @Nullable List<String> resourceTypes) {
    var filterNames = List.of(FILTER_NAME_MODULE, FILTER_NAME_CATEGORY, FILTER_NAME_RESOURCE_TYPE);
    if (!filterNames.contains(targetFilter)) {
      throw new IllegalArgumentException("Unknown filter");
    }

    return List.of(getFilter(targetFilter, keyFieldFor(targetFilter), baseFieldFor(targetFilter), searchTerm, modules, categories, resourceTypes));
  }

  private static String keyFieldFor(String name) {
    return switch (name) {
      case FILTER_NAME_MODULE -> FILTER_KEY_MODULE;
      case FILTER_NAME_CATEGORY -> FILTER_KEY_CATEGORIES;
      case FILTER_NAME_RESOURCE_TYPE -> FILTER_KEY_RESOURCE_TYPE;
      default -> throw new IllegalArgumentException("Unknown filter");
    };
  }

  private static String baseFieldFor(String name) {
    return switch (name) {
      case FILTER_NAME_MODULE -> FIELD_BASE_MODULE;
      case FILTER_NAME_CATEGORY -> FIELD_BASE_CATEGORIES;
      case FILTER_NAME_RESOURCE_TYPE -> FIELD_BASE_RESOURCE_TYPE;
      default -> throw new IllegalArgumentException("Unknown filter");
    };
  }

  private ProfileFilter getFilter(String name,
                                   String field,
                                   String baseField,
                                   @Nullable String searchTerm,
                                   @Nullable List<String> modules,
                                   @Nullable List<String> categories,
                                   @Nullable List<String> resourceTypes) {
    var sampleAggregation = Aggregation.of(sa -> sa
        .topHits(th -> th
            .size(1)
            .source(sc -> sc.filter(f -> f.includes(baseField)))));

    var queryBuilder = NativeQuery.builder()
        .withAggregation(field, Aggregation.of(a -> a
            .terms(ta -> ta.field(field).size(50))
            .aggregations("sample", sampleAggregation)))
        .withMaxResults(0);

    boolean anyListHasValues = Stream.of(modules, categories, resourceTypes)
        .anyMatch(list -> list != null && !list.isEmpty());
    boolean hasSearchTerm = searchTerm != null && !searchTerm.isBlank();

    if (anyListHasValues || hasSearchTerm) {
      queryBuilder.withQuery(Query.of(q -> q
          .bool(b -> {
            if (!(CollectionUtils.isEmpty(modules) || name.equalsIgnoreCase(FILTER_NAME_MODULE))) {
              b.filter(f -> f.terms(t -> t.field(FILTER_KEY_MODULE)
                  .terms(tv -> tv.value(modules.stream().map(FieldValue::of).toList()))));
            }
            if (!(CollectionUtils.isEmpty(categories) || name.equalsIgnoreCase(FILTER_NAME_CATEGORY))) {
              b.filter(f -> f.terms(t -> t.field(FILTER_KEY_CATEGORIES)
                  .terms(tv -> tv.value(categories.stream().map(FieldValue::of).toList()))));
            }
            if (!(CollectionUtils.isEmpty(resourceTypes) || name.equalsIgnoreCase(FILTER_NAME_RESOURCE_TYPE))) {
              b.filter(f -> f.terms(t -> t.field(FILTER_KEY_RESOURCE_TYPE)
                  .terms(tv -> tv.value(resourceTypes.stream().map(FieldValue::of).toList()))));
            }
            if (hasSearchTerm) {
              b.must(buildKeywordMatchQuery(searchTerm));
            }

            return b;
          })));
    }

    var aggregationQuery = queryBuilder.build();

    if (aggregationQuery.getQuery() != null) {
      log.info(aggregationQuery.getQuery().toString());
    }

    SearchHits<ProfileDocument> searchHits = operations.search(aggregationQuery, ProfileDocument.class);
    ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();
    assert aggregations != null;
    List<StringTermsBucket> buckets = aggregations.aggregationsAsMap().get(field).aggregation().getAggregate().sterms().buckets().array();

    List<ProfileFilterValue> values = new ArrayList<>();
    buckets.forEach(b -> {
      var key = b.key().stringValue();
      if (!key.isEmpty()) {
        values.add(ProfileFilterValue.builder()
            .display(resolveFilterValueDisplay(b, baseField, key))
            .count(b.docCount())
            .build());
      }
    });

    return ProfileFilter.builder()
        .name(name)
        .values(values)
        .build();
  }

  private DisplayEntry resolveFilterValueDisplay(StringTermsBucket bucket, String baseField, String key) {
    var hits = bucket.aggregations().get("sample").topHits().hits().hits();
    if (hits.isEmpty()) {
      return DisplayEntry.builder().original(key).translations(List.of()).build();
    }

    var sample = hits.get(0).source().to(FilterValueSample.class);

    if (FIELD_BASE_CATEGORIES.equals(baseField)) {
      var categories = sample.categories();
      return (categories == null ? List.<RawDisplay>of() : categories).stream()
          .filter(c -> c != null && key.equals(c.original()))
          .findFirst()
          .map(RawDisplay::toDisplayEntry)
          .orElseGet(() -> DisplayEntry.builder().original(key).translations(List.of()).build());
    }

    var display = FIELD_BASE_MODULE.equals(baseField) ? sample.module() : sample.resourceType();
    return display != null ? display.toDisplayEntry() : DisplayEntry.builder().original(key).translations(List.of()).build();
  }

  private record RawLocalization(
      @JsonProperty("de-DE") String deDe,
      @JsonProperty("en-US") String enUs
  ) {
  }

  private record RawDisplay(String original, RawLocalization localization) {
    DisplayEntry toDisplayEntry() {
      return DisplayEntry.builder()
          .original(original)
          .translations(List.of(
              LocalizedValue.builder().language("de-DE").value(localization == null ? null : localization.deDe()).build(),
              LocalizedValue.builder().language("en-US").value(localization == null ? null : localization.enUs()).build()
          ))
          .build();
    }
  }

  private record FilterValueSample(RawDisplay module, RawDisplay resourceType, List<RawDisplay> categories) {
  }

  private SearchHits<ProfileDocument> findByNameOrDisplay(String keyword,
                                                           List<Pair<String, List<String>>> filterList,
                                                           PageRequest pageRequest) {
    List<Query> filterTerms = new ArrayList<>();
    filterTerms.add(new TermQuery.Builder().field(FIELD_SELECTABLE).value(true).build()._toQuery());

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
          .filter(filterTerms)
          .build();

    } else {
      boolQuery = new BoolQuery.Builder()
          .must(buildKeywordMatchQuery(keyword))
          .filter(filterTerms)
          .build();
    }

    var query = new NativeQueryBuilder()
        .withQuery(boolQuery._toQuery())
        .withPageable(pageRequest)
        .build();

    log.info(Objects.requireNonNull(query.getQuery()).toString());

    return operations.search(query, ProfileDocument.class);
  }

  private Query buildKeywordMatchQuery(String keyword) {
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

    return Query.of(q -> q
        .bool(b -> b
            .should(translatedMatch._toQuery(), originalMatch._toQuery())
            .minimumShouldMatch("1")));
  }
}
