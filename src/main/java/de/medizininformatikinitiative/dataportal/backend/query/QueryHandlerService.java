package de.medizininformatikinitiative.dataportal.backend.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.medizininformatikinitiative.dataportal.backend.query.api.Query;
import de.medizininformatikinitiative.dataportal.backend.query.api.*;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.*;
import de.medizininformatikinitiative.dataportal.backend.query.dispatch.QueryDispatchException;
import de.medizininformatikinitiative.dataportal.backend.query.dispatch.QueryDispatcher;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.*;
import de.medizininformatikinitiative.dataportal.backend.query.result.RandomSiteNameGenerator;
import de.medizininformatikinitiative.dataportal.backend.query.result.ResultLine;
import de.medizininformatikinitiative.dataportal.backend.query.result.ResultService;
import de.medizininformatikinitiative.dataportal.backend.query.translation.QueryTranslationException;
import de.medizininformatikinitiative.dataportal.backend.query.translation.QueryTranslator;
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
                             @NonNull @Qualifier("cql") QueryTranslator queryTranslator,
                             @NonNull ObjectMapper jsonUtil) {
    this.queryDispatcher = queryDispatcher;
    this.queryRepository = queryRepository;
    this.queryContentRepository = queryContentRepository;
    this.resultService = resultService;
    this.queryTranslator = queryTranslator;
    this.jsonUtil = jsonUtil;
  }

  private final QueryDispatcher queryDispatcher;

  private final QueryRepository queryRepository;

  private final QueryContentRepository queryContentRepository;

  private final ResultService resultService;

  private QueryTranslator queryTranslator;

  private ObjectMapper jsonUtil;

  public Mono<Long> runQuery(Ccdl ccdl, String userId) {
    try {
      var queryId = queryDispatcher.enqueueNewQuery(ccdl, userId);
      return queryDispatcher.dispatchEnqueuedQuery(queryId)
          .thenReturn(queryId);
    } catch (QueryDispatchException e) {
      return Mono.error(e);
    }
  }

  @Transactional
  public QueryResult getQueryResult(Long queryId, de.medizininformatikinitiative.dataportal.backend.query.QueryHandlerService.ResultDetail resultDetail) {
    var singleSiteResults = resultService.findSuccessfulByQuery(queryId);
    List<QueryResultLine> resultLines = new ArrayList<>();

    if (resultDetail != de.medizininformatikinitiative.dataportal.backend.query.QueryHandlerService.ResultDetail.SUMMARY) {
      resultLines = singleSiteResults.stream()
          .map(ssr -> QueryResultLine.builder()
              .siteName(resultDetail == de.medizininformatikinitiative.dataportal.backend.query.QueryHandlerService.ResultDetail.DETAILED_OBFUSCATED ? RandomSiteNameGenerator.generateRandomSiteName() : ssr.siteName())
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

  public Ccdl getQueryContent(Long queryId) throws JsonProcessingException {
    var queryContent = queryContentRepository.findByQueryId(queryId);
    if (queryContent.isPresent()) {
      return jsonUtil.readValue(queryContent.get().getQueryContent(), Ccdl.class);
    } else {
      return null;
    }
  }


  private Query convertQueryToApi(de.medizininformatikinitiative.dataportal.backend.query.persistence.Query in)
      throws JsonProcessingException {

    return Query.builder()
        .id(in.getId())
        .content(jsonUtil.readValue(in.getQueryContent().getQueryContent(), Ccdl.class))
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

  public String translateQueryToCql(Ccdl ccdl) throws QueryTranslationException {
    return queryTranslator.translate(ccdl);
  }
}
