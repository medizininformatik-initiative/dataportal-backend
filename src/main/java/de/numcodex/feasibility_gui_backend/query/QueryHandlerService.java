package de.numcodex.feasibility_gui_backend.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import de.numcodex.feasibility_gui_backend.query.api.Query;
import de.numcodex.feasibility_gui_backend.query.api.*;
import de.numcodex.feasibility_gui_backend.query.api.status.IssueWrapper;
import de.numcodex.feasibility_gui_backend.query.api.status.QueryQuota;
import de.numcodex.feasibility_gui_backend.query.api.status.QueryQuotaEntry;
import de.numcodex.feasibility_gui_backend.query.api.validation.JsonSchemaValidator;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatcher;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import de.numcodex.feasibility_gui_backend.query.result.RandomSiteNameGenerator;
import de.numcodex.feasibility_gui_backend.query.result.ResultLine;
import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslationException;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslator;
import de.numcodex.feasibility_gui_backend.terminology.validation.StructuredQueryValidation;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.threeten.extra.PeriodDuration;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueryHandlerService {

    public enum ResultDetail {
        SUMMARY,
        DETAILED_OBFUSCATED,
        DETAILED
    }

    public QueryHandlerService(@NonNull QueryDispatcher queryDispatcher,
                               @NonNull QueryRepository queryRepository,
                               @NonNull QueryContentRepository queryContentRepository,
                               @NonNull ResultService resultService,
                               @NonNull StructuredQueryValidation structuredQueryValidation,
                               @NonNull @Qualifier("cql") QueryTranslator queryTranslator,
                               @NonNull JsonSchemaValidator jsonSchemaValidator,
                               @NonNull ObjectMapper jsonUtil) {
        this.queryDispatcher = queryDispatcher;
        this.queryRepository = queryRepository;
        this.queryContentRepository = queryContentRepository;
        this.resultService = resultService;
        this.structuredQueryValidation = structuredQueryValidation;
        this.queryTranslator = queryTranslator;
        this.jsonSchemaValidator = jsonSchemaValidator;
        this.jsonUtil = jsonUtil;
    }

    private final QueryDispatcher queryDispatcher;

    private final QueryRepository queryRepository;

    private final QueryContentRepository queryContentRepository;

    private final ResultService resultService;

    private final StructuredQueryValidation structuredQueryValidation;

    private QueryTranslator queryTranslator;

    private final JsonSchemaValidator jsonSchemaValidator;

    private ObjectMapper jsonUtil;

    public Mono<Long> runQuery(StructuredQuery structuredQuery, String userId) {
        try {
            var queryId = queryDispatcher.enqueueNewQuery(structuredQuery, userId);
            return queryDispatcher.dispatchEnqueuedQuery(queryId)
                    .thenReturn(queryId);
        } catch (QueryDispatchException e) {
            return Mono.error(e);
        }
    }

    @Transactional
    public QueryResult getQueryResult(Long queryId, ResultDetail resultDetail) {
        var singleSiteResults = resultService.findSuccessfulByQuery(queryId);
        List<QueryResultLine> resultLines = new ArrayList<>();

        if (resultDetail != ResultDetail.SUMMARY) {
            resultLines = singleSiteResults.stream()
                .map(ssr -> QueryResultLine.builder()
                    .siteName(resultDetail == ResultDetail.DETAILED_OBFUSCATED ? RandomSiteNameGenerator.generateRandomSiteName() : ssr.siteName())
                    .numberOfPatients(ssr.result())
                    .build())
                .toList();
        }

        var totalMatchesInPopulation = singleSiteResults.stream()
            .mapToLong(ResultLine::result).sum();

        return QueryResult.builder()
            .queryId(queryId)
            .resultLines(resultLines)
            .totalNumberOfPatients(totalMatchesInPopulation)
            .build();
    }

    public Query getQuery(Long queryId) throws JsonProcessingException {
        var query = queryRepository.findById(queryId);
        if (query.isPresent()) {
            return convertQueryToApi(query.get());
        } else {
            return null;
        }
    }

    public StructuredQuery getQueryContent(Long queryId) throws JsonProcessingException {
        var queryContent = queryContentRepository.findByQueryId(queryId);
        if (queryContent.isPresent()) {
            return jsonUtil.readValue(queryContent.get().getQueryContent(), StructuredQuery.class);
        } else {
            return null;
        }
    }


    private Query convertQueryToApi(de.numcodex.feasibility_gui_backend.query.persistence.Query in)
        throws JsonProcessingException {

        return Query.builder()
                .id(in.getId())
                .content(jsonUtil.readValue(in.getQueryContent().getQueryContent(), StructuredQuery.class))
                .build();
    }

    public String getAuthorId(Long queryId) throws QueryNotFoundException {
        return queryRepository.getAuthor(queryId).orElseThrow(QueryNotFoundException::new);
    }

    public Long getAmountOfQueriesByUserAndInterval(String userId, String interval) {
        return queryRepository.countQueriesByAuthorInTheLastNMinutes(userId, PeriodDuration.parse(interval).getDuration().toMinutes());
    }

    public Long getRetryAfterTime(String userId, int offset, String interval) {
        try {
            return PeriodDuration.parse(interval).getDuration().getSeconds() - queryRepository.getAgeOfNToLastQueryInSeconds(userId, offset) + 1;
        } catch (NullPointerException e) {
            return 0L;
        }
    }

  public QueryQuota getSentQueryStatistics(String userName, int softAmount, String softInterval, int hardAmount, String hardInterval) {
    var softUsed = queryRepository.countQueriesByAuthorInTheLastNMinutes(userName, PeriodDuration.parse(softInterval).getDuration().toMinutes());
    var hardUsed = queryRepository.countQueriesByAuthorInTheLastNMinutes(userName, PeriodDuration.parse(hardInterval).getDuration().toMinutes());

    return QueryQuota.builder()
        .soft(QueryQuotaEntry.builder()
            .interval(softInterval)
            .limit(softAmount)
            .used(softUsed.intValue())
            .build())
        .hard(QueryQuotaEntry.builder()
            .interval(hardInterval)
            .limit(hardAmount)
            .used(hardUsed.intValue())
            .build())
        .build();
  }

  public String translateQueryToCql(StructuredQuery structuredQuery) throws QueryTranslationException {
        return queryTranslator.translate(structuredQuery);
  }

  public List<IssueWrapper> validateCcdl(JsonNode ccdlNode) {
    List<IssueWrapper> issues = new ArrayList<>();
    var validationErrors = jsonSchemaValidator.validate(JsonSchemaValidator.SCHEMA_CCDL, ccdlNode);
    if (!validationErrors.isEmpty()) {
      issues = validationErrors.stream()
          .map(e -> new IssueWrapper(e.getInstanceLocation().toString(), e.getMessage()))
          .toList();
    }
    return issues;
  }

  public StructuredQuery ccdlFromJsonNode(JsonNode jsonNode) {
      return jsonUtil.convertValue(jsonNode, StructuredQuery.class);
  }
}
