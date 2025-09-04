package de.numcodex.feasibility_gui_backend.terminology.es;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.api.RelationEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.model.*;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemEsRepository;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemNotFoundException;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyListItemEsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

@Service
@Slf4j
@ConditionalOnExpression("${app.elastic.enabled}")
public class TerminologyEsService {
  public static final String FILTER_KEY_CRITERIA_SETS = "criteria_sets";
  public static final String FILTER_KEY_CONTEXT_CODE = "context.code";
  public static final String FILTER_KEY_KDS_MODULE = "kds_module";
  public static final String FILTER_KEY_TERMINOLOGY = "terminology";
  public static final String FIELD_NAME_DISPLAY_DE = "display.de";
  public static final String FIELD_NAME_DISPLAY_EN = "display.en";
  public static final String FIELD_NAME_DISPLAY_ORIGINAL = "display.original";
  public static final String FIELD_NAME_TERMCODE_WITH_BOOST = "termcode^2";
  private static final UUID NAMESPACE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private ElasticsearchOperations operations;

  private String[] filterFields;

  private OntologyItemEsRepository ontologyItemEsRepository;

  private OntologyListItemEsRepository ontologyListItemEsRepository;

  @Autowired
  public TerminologyEsService(@Value("${app.elastic.filter}") String[] filterFields, ElasticsearchOperations operations, OntologyItemEsRepository ontologyItemEsRepository, OntologyListItemEsRepository ontologyListItemEsRepository) {
    this.filterFields = filterFields;
    this.operations = operations;
    this.ontologyItemEsRepository = ontologyItemEsRepository;
    this.ontologyListItemEsRepository = ontologyListItemEsRepository;
  }

  public EsSearchResultEntry getSearchResultEntryByCriterion(Criterion criterion) {
    var contextualizedTermcodeHash = createContextualizedTermcodeHash(criterion);
    return getSearchResultEntryByHash(contextualizedTermcodeHash);
  }

  public EsSearchResultEntry getSearchResultEntryByHash(String hash) {
    var ontologyItem = ontologyListItemEsRepository.findById(hash).orElseThrow(OntologyItemNotFoundException::new);
    return EsSearchResultEntry.of(ontologyItem);
  }

  public List<EsSearchResultEntry> getSearchResultEntriesByHash(List<String> hashes) {
    var ontologyItems = ontologyListItemEsRepository.findAllById(hashes);
    List<EsSearchResultEntry> results = new ArrayList<>();
    ontologyItems.forEach(oi  -> results.add(EsSearchResultEntry.of(oi)));
    return results;
  }

  public List<TermFilter> getAvailableFilters() {
    var filterTerms = List.of(filterFields);
    var list = new ArrayList<TermFilter>();

    for (var term : filterTerms) {
      list.add(getFilter(term));
    }

    // Hardcode availability filter. It is not yet decided if and how this will be available anyways. TODO!
    list.add(TermFilter.builder()
        .type("boolean")
        .name("availability")
        .values(List.of())
        .build());

    return list;
  }

  public EsSearchResult performOntologySearchWithPaging(String keyword,
                                                        @Nullable List<String> criteriaSets,
                                                        @Nullable List<String> context,
                                                        @Nullable List<String> kdsModule,
                                                        @Nullable List<String> terminology,
                                                        @Nullable boolean availability,
                                                        @Nullable int pageSize,
                                                        @Nullable int page) {


    List<Pair<String, List<String>>> filterList = new ArrayList<>();
    if (!CollectionUtils.isEmpty(criteriaSets)) {
      filterList.add(Pair.of(FILTER_KEY_CRITERIA_SETS, criteriaSets));
    }
    if (!CollectionUtils.isEmpty(context)) {
      filterList.add(Pair.of(FILTER_KEY_CONTEXT_CODE, context));
    }
    if (!CollectionUtils.isEmpty(kdsModule)) {
      filterList.add(Pair.of(FILTER_KEY_KDS_MODULE, kdsModule));
    }
    if (!CollectionUtils.isEmpty(terminology)) {
      filterList.add(Pair.of(FILTER_KEY_TERMINOLOGY, terminology));
    }

    SearchHits<OntologyListItemDocument> searchHitPage = findByNameOrTermcode(
        keyword,
        filterList,
        availability,
        PageRequest.of(page, pageSize)
    );

    List<EsSearchResultEntry> ontologyItems = new ArrayList<>();

    searchHitPage.getSearchHits().forEach(hit -> ontologyItems.add(EsSearchResultEntry.of(hit.getContent())));
    return EsSearchResult.builder()
        .totalHits(searchHitPage.getTotalHits())
        .results(ontologyItems)
        .build();
  }

  private SearchHits<OntologyListItemDocument> findByNameOrTermcode(String keyword,
                                                                    List<Pair<String,List<String>>> filterList,
                                                                    boolean availability,
                                                                    PageRequest pageRequest) {

    List<Query> filterTerms = new ArrayList<>();

    if (availability) {
      var availabilityFilter = RangeQuery.of(r -> r
          .number(n -> n
              .field("availability")
              .gt(0.0)
          )
      );
      filterTerms.add(availabilityFilter._toQuery());
    }

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
      var translationDeExistsQuery = new ExistsQuery.Builder()
          .field(FIELD_NAME_DISPLAY_DE)
          .build();

      var translationEnExistsQuery = new ExistsQuery.Builder()
          .field(FIELD_NAME_DISPLAY_EN)
          .build();

      var mmQueryWithTranslations = new MultiMatchQuery.Builder()
          .query(keyword)
          .fields(List.of(FIELD_NAME_DISPLAY_DE, FIELD_NAME_DISPLAY_EN, FIELD_NAME_TERMCODE_WITH_BOOST))
          .build();

      var boolQueryWithTranslations = new BoolQuery.Builder()
          .should(List.of(translationDeExistsQuery._toQuery(), translationEnExistsQuery._toQuery()))
          .must(mmQueryWithTranslations._toQuery())
          .build();

      var mmQueryWithOriginal = new MultiMatchQuery.Builder()
          .query(keyword)
          .fields(List.of(FIELD_NAME_DISPLAY_ORIGINAL, FIELD_NAME_TERMCODE_WITH_BOOST))
          .build();

      var boolQueryWithOriginal = new BoolQuery.Builder()
          .mustNot(List.of(translationDeExistsQuery._toQuery(), translationEnExistsQuery._toQuery()))
          .must(mmQueryWithOriginal._toQuery())
          .build();

      boolQuery = new BoolQuery.Builder()
          .should(List.of(boolQueryWithTranslations._toQuery(), boolQueryWithOriginal._toQuery()))
          .filter(filterTerms.isEmpty() ? List.of() : filterTerms)
          .minimumShouldMatch("1")
          .build();
    }

    var innerQuery = new NativeQueryBuilder()
        .withQuery(boolQuery._toQuery())
        .withPageable(pageRequest)
        .build();

    var availabilityScoreScript = new Script.Builder()
        .source("doc['availability'].value == 0 ? _score : _score + 100")
        .build();

    var function = FunctionScoreBuilders.scriptScore()
        .script(availabilityScoreScript)
        .build();

    var functionList = List.of(function._toFunctionScore());

    var functionScoreQuery = new FunctionScoreQuery.Builder()
        .query(innerQuery.getQuery())
        .functions(functionList)
        .boostMode(FunctionBoostMode.Replace)
        .build();

    var finalQuery = new NativeQueryBuilder()
        .withQuery(functionScoreQuery._toQuery())
        .withPageable(pageRequest)
        .build();

    log.info(finalQuery.getQuery().toString());

    return operations.search(finalQuery, OntologyListItemDocument.class);

  }

  public RelationEntry getRelationEntryByHash(String hash) {
    var ontologyItem = ontologyItemEsRepository.findById(hash).orElseThrow(OntologyItemNotFoundException::new);
    var ontologyItemRelationsDocument = OntologyItemRelationsDocument.builder()
        .display(ontologyItem.display())
        .parents(ontologyItem.parents())
        .children(ontologyItem.children())
        .relatedTerms(ontologyItem.relatedTerms())
        .build();
    return RelationEntry.of(ontologyItemRelationsDocument);
  }


  private TermFilter getFilter(String termApi) {
    final var termElastic = termApi.equalsIgnoreCase("context") ? "context.code" : termApi;
    var aggregationQuery = NativeQuery.builder()
        .withAggregation(termElastic, Aggregation.of(a -> a
            .terms(ta -> ta.field(termElastic).size(50))))
        .withMaxResults(0)
        .build();

    SearchHits<OntologyListItemDocument> searchHits = operations.search(aggregationQuery, OntologyListItemDocument.class);
    ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();
    assert aggregations != null;
    List<StringTermsBucket> buckets = aggregations.aggregationsAsMap().get(termElastic).aggregation().getAggregate().sterms().buckets().array();
    List<TermFilterValue> termFilterValues = new ArrayList<>();

    buckets.forEach(b -> {
      if (!b.key().stringValue().isEmpty()) {
        termFilterValues.add(TermFilterValue.builder().label(b.key().stringValue()).count(b.docCount()).build());
      }
    });

    return TermFilter.builder()
        .name(termApi)
        .type("selectable-concept")
        .values(termFilterValues)
        .build();
  }

  public static String createContextualizedTermcodeHash(Criterion criterion) {
    String contextualizedTermcode = MessageFormat.format("{0}{1}{2}{3}{4}",
        criterion.context().system(),
        criterion.context().code(),
        (criterion.context().version() == null || criterion.context().version().isBlank()) ? "" : criterion.context().version(),
        criterion.termCodes().get(0).system(),
        criterion.termCodes().get(0).code()
    );
    return createUuidV3(NAMESPACE_UUID, contextualizedTermcode).toString();
  }

  public static UUID createUuidV3(UUID namespace, String subject) {
    final byte[] nameBytes = subject.getBytes(StandardCharsets.UTF_8);

    ByteBuffer buffer = ByteBuffer.allocate(nameBytes.length + 16);
    buffer.putLong(namespace.getMostSignificantBits());
    buffer.putLong(namespace.getLeastSignificantBits());
    buffer.put(nameBytes);

    return UUID.nameUUIDFromBytes(buffer.array());
  }
}
