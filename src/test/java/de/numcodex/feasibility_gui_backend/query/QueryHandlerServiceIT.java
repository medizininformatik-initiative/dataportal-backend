package de.numcodex.feasibility_gui_backend.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Unit;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail;
import de.numcodex.feasibility_gui_backend.query.api.QueryResultLine;
import de.numcodex.feasibility_gui_backend.query.api.Ccdl;
import de.numcodex.feasibility_gui_backend.query.api.ValueFilter;
import de.numcodex.feasibility_gui_backend.query.api.status.QueryQuota;
import de.numcodex.feasibility_gui_backend.query.api.validation.JsonSchemaValidator;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerSpringConfig;
import de.numcodex.feasibility_gui_backend.query.collect.QueryCollectSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dataquery.DataqueryCsvExportService;
import de.numcodex.feasibility_gui_backend.query.dataquery.DataquerySpringConfig;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryHashCalculator;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import de.numcodex.feasibility_gui_backend.query.result.ResultLine;
import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import de.numcodex.feasibility_gui_backend.query.result.ResultServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslatorSpringConfig;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import de.numcodex.feasibility_gui_backend.terminology.validation.CcdlValidation;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.threeten.extra.PeriodDuration;

import static de.numcodex.feasibility_gui_backend.common.api.Comparator.GREATER_EQUAL;
import static de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail.DETAILED;
import static de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail.DETAILED_OBFUSCATED;
import static de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail.SUMMARY;
import static de.numcodex.feasibility_gui_backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.ERROR;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag("query")
@Tag("handler")
@Import({
        BrokerSpringConfig.class,
        QueryTranslatorSpringConfig.class,
        QueryDispatchSpringConfig.class,
        QueryCollectSpringConfig.class,
        QueryHandlerService.class,
        ResultServiceSpringConfig.class,
        DataquerySpringConfig.class
})
@DataJpaTest(
        properties = {
                "app.cqlTranslationEnabled=true",
                "app.fhirTranslationEnabled=false",
                "app.broker.mock.enabled=true",
                "app.broker.direct.enabled=false",
                "app.broker.aktin.enabled=false",
                "app.broker.dsf.enabled=false"
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class QueryHandlerServiceIT {

    public static final String SITE_NAME_1 = "site-name-114606";
    public static final String SITE_NAME_2 = "site-name-114610";
    public static final String CREATOR = "creator-114634";
    public static final long UNKNOWN_QUERY_ID = 9999999L;
    public static final String TIME_STRING = "1969-07-20 20:17:40.0";

    @Autowired
    private QueryHandlerService queryHandlerService;

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private QueryContentRepository queryContentRepository;

    @Autowired
    private QueryDispatchRepository queryDispatchRepository;

    @Autowired
    private ResultService resultService;

    @Autowired
    private QueryHashCalculator queryHashCalculator;

    @MockitoBean
    private DataqueryCsvExportService dataqueryCsvExportService;

    @MockitoBean
    private CcdlValidation ccdlValidation;

    @MockitoBean
    private JsonSchemaValidator jsonSchemaValidator;

    @Autowired
    @Qualifier("translation")
    private ObjectMapper jsonUtil;

    @Test
    public void testRunQuery() {
        var testCcdl = createValidCcdl();

        queryHandlerService.runQuery(testCcdl, "test").block();

        assertThat(queryRepository.count()).isOne();
        assertThat(queryDispatchRepository.count()).isOne();
    }

    @Test
    public void testGetQuery_succeeds() throws JsonProcessingException {
        var fakeContent = new QueryContent("{}");
        fakeContent.setHash("a2189dffb");
        queryContentRepository.save(fakeContent);
        var query = new Query();
        query.setCreatedBy(CREATOR);
        query.setQueryContent(fakeContent);
        var queryId = queryRepository.save(query).getId();

        var loadedQuery = queryHandlerService.getQuery(queryId);

        assertThat(loadedQuery).isNotNull();
        assertThat(jsonUtil.writeValueAsString(loadedQuery.content())).isEqualTo(fakeContent.getQueryContent());
    }

    @Test
    public void testGetQuery_UnknownQueryIdReturnsNull() throws JsonProcessingException {
        var query = queryHandlerService.getQuery(UNKNOWN_QUERY_ID);

        assertThat(query).isNull();
    }

    @Test
    public void testGetQueryContent_succeeds() throws JsonProcessingException {
        var fakeContent = new QueryContent("{}");
        fakeContent.setHash("a2189dffb");
        queryContentRepository.save(fakeContent);
        var query = new Query();
        query.setCreatedBy(CREATOR);
        query.setQueryContent(fakeContent);
        var queryId = queryRepository.save(query).getId();

        var loadedQueryContent = queryHandlerService.getQueryContent(queryId);

        assertThat(loadedQueryContent).isNotNull();
        assertThat(jsonUtil.writeValueAsString(loadedQueryContent)).isEqualTo(fakeContent.getQueryContent());
    }

    @Test
    public void testGetQueryContent_UnknownQueryIdReturnsNull() throws JsonProcessingException {
        var queryContent = queryHandlerService.getQueryContent(UNKNOWN_QUERY_ID);

        assertThat(queryContent).isNull();
    }

    // This behavior seems to be necessary since the UI is polling constantly.
    // If the response was an error then the UI would need to handle it accordingly.
    // TODO: We should discuss this with the UI team. Maybe a better solution can be identified.
    @Test
    public void testGetQueryResult_UnknownQueryIdLeadsToResultWithZeroMatchesInPopulation() {
        var queryResult = queryHandlerService.getQueryResult(UNKNOWN_QUERY_ID, DETAILED_OBFUSCATED);

        assertThat(queryResult.queryId()).isEqualTo(UNKNOWN_QUERY_ID);
        assertThat(queryResult.totalNumberOfPatients()).isZero();
        assertThat(queryResult.resultLines()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource
    public void testGetQueryResult_ErrorResultsAreIgnored(ResultDetail resultDetail) {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_1)
                        .type(ERROR)
                        .result(0L)
                        .build()
        );

        var queryResult = queryHandlerService.getQueryResult(queryId, resultDetail);

        assertThat(queryResult.resultLines()).isEmpty();
    }

    @Test
    public void testGetQueryResult_SummaryContainsOnlyTheTotal() {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_1)
                        .type(SUCCESS)
                        .result(10L)
                        .build());
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_2)
                        .type(SUCCESS)
                        .result(20L)
                        .build()
        );

        var queryResult = queryHandlerService.getQueryResult(queryId, SUMMARY);

        assertThat(queryResult.totalNumberOfPatients()).isEqualTo(30L);
        assertThat(queryResult.resultLines()).isEmpty();
    }

    @Test
    public void testGetQueryResult_DetailedObfuscatedDoesNotContainTheSiteNames() {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_1)
                        .type(SUCCESS)
                        .result(10L)
                        .build()
        );
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_2)
                        .type(SUCCESS)
                        .result(20L)
                        .build()
        );

        var queryResult = queryHandlerService.getQueryResult(queryId, DETAILED_OBFUSCATED);

        assertThat(queryResult.totalNumberOfPatients()).isEqualTo(30L);
        assertThat(queryResult.resultLines()).hasSize(2);
        assertThat(queryResult.resultLines().stream().map(QueryResultLine::siteName))
            .doesNotContain(SITE_NAME_1, SITE_NAME_2);
        assertThat(queryResult.resultLines().stream().map(QueryResultLine::numberOfPatients))
            .contains(10L, 20L);
    }

    @Test
    public void testGetQueryResult_DetailedContainsTheSiteNames() {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_1)
                        .type(SUCCESS)
                        .result(10L)
                        .build()
        );
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_2)
                        .type(SUCCESS)
                        .result(20L)
                        .build()
        );

        var queryResult = queryHandlerService.getQueryResult(queryId, DETAILED);

        assertThat(queryResult.totalNumberOfPatients()).isEqualTo(30L);
        assertThat(queryResult.resultLines())
            .hasSize(2)
            .contains(QueryResultLine.builder().siteName(SITE_NAME_1).numberOfPatients(10L).build(),
                QueryResultLine.builder().siteName(SITE_NAME_2).numberOfPatients(20L).build());
    }

    @Test
    public void testGetQuery_nullOnNotFound() throws JsonProcessingException {
        var queryFromDb = queryHandlerService.getQuery(1L);

        assertThat(queryFromDb).isNull();
    }

    @Test
    public void testGetQuery_succeess() throws JsonProcessingException {
        var queryContentString = jsonUtil.writeValueAsString(createValidCcdl());
        var queryContentHash = queryHashCalculator.calculateSerializedQueryBodyHash(queryContentString);
        var queryContent = new QueryContent(queryContentString);
        queryContent.setHash(queryContentHash);
        var query = new Query();
        query.setCreatedBy(CREATOR);
        query.setQueryContent(queryContent);
        var queryId = queryRepository.save(query).getId();

        var queryFromDb = queryHandlerService.getQuery(queryId);

        assertThat(queryFromDb.label()).isNull();
        assertThat(queryFromDb.comment()).isNull();
        assertThat(queryFromDb.content().inclusionCriteria()).isEqualTo(createValidCcdl().inclusionCriteria());
    }

    @Test
    public void testGetQueryContent_nullIfNotFound() throws JsonProcessingException {
        var queryContentString = jsonUtil.writeValueAsString(createValidCcdl());
        var queryContentHash = queryHashCalculator.calculateSerializedQueryBodyHash(queryContentString);
        var queryContent = new QueryContent(queryContentString);
        queryContent.setHash(queryContentHash);
        var query = new Query();
        query.setCreatedBy(CREATOR);
        query.setQueryContent(queryContent);
        var queryId = queryRepository.save(query).getId();

        var queryContentFromDb = queryHandlerService.getQueryContent(++queryId);

        assertThat(queryContentFromDb).isNull();
    }

    @Test
    public void testGetAuthorId_UnknownQueryIdThrows() {
        assertThrows(QueryNotFoundException.class,
            () -> queryHandlerService.getAuthorId(UNKNOWN_QUERY_ID));
    }

    @Test
    public void testGetAmountOfQueriesByUserAndInterval() throws JsonProcessingException {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        queryRepository.save(query).getId();

        var count0 = queryHandlerService.getAmountOfQueriesByUserAndInterval(CREATOR, "PT0M");
        var count1 = queryHandlerService.getAmountOfQueriesByUserAndInterval(CREATOR, "PT1M");

        assertThat(count0).isEqualTo(0);
        assertThat(count1).isEqualTo(1);
    }

    @Test
    @DisplayName("getRetryAfterTime() -> return 0 on empty")
    public void getRetryAfterTime_zeroOnEmpty() {
        Long retryAfterTime = queryHandlerService.getRetryAfterTime(CREATOR, 0, "PT1000000M");
        assertThat(retryAfterTime).isEqualTo(0L);
    }

    @Test
    @DisplayName("getRetryAfterTime() -> return >0 on non empty")
    public void getRetryAfterTime_nonZeroOnNotEmpty() {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        queryRepository.save(query);
        Long retryAfterTime = queryHandlerService.getRetryAfterTime(CREATOR, 0, "PT1000000M");
        assertThat(retryAfterTime).isGreaterThan(0L);
    }

  @Test
  @DisplayName("getSentQueryStatistics() -> succeeds with no entries")
  public void getSentQueryStatistics_succeedsWithNoEntries(@Value("${app.privacy.quota.hard.create.interval}") String hardInterval,
                                                           @Value("${app.privacy.quota.soft.create.interval}") String softInterval,
                                                           @Value("${app.privacy.quota.hard.create.amount}") int hardLimit,
                                                           @Value("${app.privacy.quota.soft.create.amount}") int softLimit) {
    var sentQueryStatistics =
        assertDoesNotThrow(() -> queryHandlerService.getSentQueryStatistics(CREATOR, softLimit, softInterval, hardLimit, hardInterval));

    assertThat(sentQueryStatistics).isInstanceOf(QueryQuota.class);
    assertThat(sentQueryStatistics.hard().interval()).isEqualTo(hardInterval);
    assertThat(sentQueryStatistics.hard().used()).isEqualTo(0);
    assertThat(sentQueryStatistics.hard().limit()).isEqualTo(hardLimit);
    assertThat(sentQueryStatistics.soft().interval()).isEqualTo(softInterval);
    assertThat(sentQueryStatistics.soft().used()).isEqualTo(0);
    assertThat(sentQueryStatistics.soft().limit()).isEqualTo(softLimit);
  }

  @Test
  @DisplayName("getSentQueryStatistics() -> succeeds with entries")
  public void getSentQueryStatistics_succeedsWithEntries(@Value("${app.privacy.quota.hard.create.interval}") String hardInterval,
                                                         @Value("${app.privacy.quota.soft.create.interval}") String softInterval,
                                                         @Value("${app.privacy.quota.hard.create.amount}") int hardLimit,
                                                         @Value("${app.privacy.quota.soft.create.amount}") int softLimit) {
    var fakeContent = new QueryContent("{}");
    fakeContent.setHash("a2189dffb");
    queryContentRepository.save(fakeContent);

    // This query is from "right now", so it should count towards both limits
    var currentQuery = new Query();
    currentQuery.setCreatedBy(CREATOR);
    currentQuery.setQueryContent(fakeContent);
    queryRepository.save(currentQuery);

    // This query is from another user and should not be included in any of the limits
    var queryFromAnotherUser = new Query();
    queryFromAnotherUser.setCreatedBy("not-the-original-" + CREATOR);
    queryFromAnotherUser.setQueryContent(fakeContent);
    queryRepository.save(queryFromAnotherUser);

    // This query is very old and should not count towards any of the limits
    var veryOldQuery = new Query();
    veryOldQuery.setCreatedBy(CREATOR);
    veryOldQuery.setQueryContent(fakeContent);
    var veryOldQueryId = queryRepository.save(veryOldQuery).getId();

    // This query is older than the soft limit but younger than the hard limit, so it should only count towards the hard limit
    var queryOnlyCountingToHardLimit = new Query();
    queryOnlyCountingToHardLimit.setCreatedBy(CREATOR);
    queryOnlyCountingToHardLimit.setQueryContent(fakeContent);
    var inbetweenQueryId = queryRepository.save(queryOnlyCountingToHardLimit).getId();

    queryRepository.updateCreationDate(veryOldQueryId, Timestamp.valueOf(TIME_STRING));
    queryRepository.updateCreationDate(inbetweenQueryId, Timestamp.valueOf(LocalDateTime.now().minus(PeriodDuration.parse(softInterval)).minusSeconds(5L)));

    var sentQueryStatistics =
        assertDoesNotThrow(() -> queryHandlerService.getSentQueryStatistics(CREATOR, softLimit, softInterval, hardLimit, hardInterval));

    assertThat(sentQueryStatistics).isInstanceOf(QueryQuota.class);
    assertThat(sentQueryStatistics.hard().interval()).isEqualTo(hardInterval);
    assertThat(sentQueryStatistics.hard().used()).isEqualTo(2);
    assertThat(sentQueryStatistics.hard().limit()).isEqualTo(hardLimit);
    assertThat(sentQueryStatistics.soft().interval()).isEqualTo(softInterval);
    assertThat(sentQueryStatistics.soft().used()).isEqualTo(1);
    assertThat(sentQueryStatistics.soft().limit()).isEqualTo(softLimit);
  }

  @Test
  @DisplayName("translateQueryToCql() -> succeeds")
  public void translateQueryToCql_succeeds() {
    var cql = assertDoesNotThrow(() -> queryHandlerService.translateQueryToCql(createValidCcdl()));

    assertThat(cql).isInstanceOf(String.class);
    AssertionsForClassTypes.assertThat(cql).containsIgnoringCase("Context Patient");
  }

  private Ccdl createValidCcdl() {
    var termCode = TermCode.builder()
        .code("424144002")
        .system("http://snomed.info/sct")
        .display("Gegenwärtiges chronologisches Alter")
        .build();
    var context = TermCode.builder()
        .code("Patient")
        .system("fdpg.mii.cds")
        .version("1.0.0")
        .display("Patient")
        .build();
    var unit = Unit.builder()
        .code("a")
        .display("a")
        .build();
    var valueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_EQUAL)
        .quantityUnit(unit)
        .value(50.0)
        .build();
    var criterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .context(context)
        .valueFilter(valueFilter)
        .build();
    return Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(criterion)))
        .build();
  }
}
