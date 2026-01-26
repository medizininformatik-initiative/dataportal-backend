package de.medizininformatikinitiative.dataportal.backend.query.v5;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.path.NodePath;
import com.networknt.schema.path.PathType;
import de.medizininformatikinitiative.dataportal.backend.common.api.Criterion;
import de.medizininformatikinitiative.dataportal.backend.common.api.TermCode;
import de.medizininformatikinitiative.dataportal.backend.common.api.Unit;
import de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig;
import de.medizininformatikinitiative.dataportal.backend.query.QueryHandlerService;
import de.medizininformatikinitiative.dataportal.backend.query.api.*;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.*;
import de.medizininformatikinitiative.dataportal.backend.query.api.validation.JsonSchemaValidator;
import de.medizininformatikinitiative.dataportal.backend.query.api.validation.CcdlValidatorSpringConfig;
import de.medizininformatikinitiative.dataportal.backend.query.dispatch.QueryDispatchException;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.UserBlacklist;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.UserBlacklistRepository;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.AuthenticationHelper;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.RateLimitingInterceptor;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.medizininformatikinitiative.dataportal.backend.query.result.ResultLine;
import de.medizininformatikinitiative.dataportal.backend.query.translation.QueryTranslationException;
import de.medizininformatikinitiative.dataportal.backend.terminology.TerminologyService;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.CodeableConceptService;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.TerminologyEsService;
import de.medizininformatikinitiative.dataportal.backend.terminology.validation.CcdlValidation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static de.medizininformatikinitiative.dataportal.backend.common.api.Comparator.GREATER_EQUAL;
import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.*;
import static de.medizininformatikinitiative.dataportal.backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static de.medizininformatikinitiative.dataportal.backend.query.persistence.ResultType.SUCCESS;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("query")
@ExtendWith(SpringExtension.class)
@Import({CcdlValidatorSpringConfig.class,
    RateLimitingServiceSpringConfig.class
})
@WebMvcTest(
    controllers = FeasibilityQueryHandlerRestController.class,
    properties = {
        "app.enableQueryValidation=false"
    }
)
@SuppressWarnings("NewClassNamingConvention")
public class FeasibilityQueryHandlerRestControllerIT {

  private static final String PATH = PATH_API + PATH_QUERY + PATH_FEASIBILITY;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper jsonUtil;

  @MockitoBean
  private QueryHandlerService queryHandlerService;

  @MockitoBean
  private CcdlValidation ccdlValidation;

  @MockitoBean
  private JsonSchemaValidator jsonSchemaValidator;

  @MockitoBean
  private RateLimitingInterceptor rateLimitingInterceptor;

  @MockitoBean
  private UserBlacklistRepository userBlacklistRepository;

  @MockitoBean
  private AuthenticationHelper authenticationHelper;

  @MockitoBean
  private TerminologyEsService terminologyEsService;

  @MockitoBean
  private TerminologyService terminologyService;

  @MockitoBean
  private CodeableConceptService codeableConceptService;

  @Value("${app.privacy.quota.soft.create.amount}")
  private int quotaSoftCreateAmount;

  @Value("${app.privacy.quota.soft.create.interval}")
  private String quotaSoftCreateInterval;

  @Value("${app.privacy.quota.hard.create.amount}")
  private int quotaHardCreateAmount;

  @Value("${app.privacy.quota.hard.create.interval}")
  private String quotaHardCreateInterval;

  @Value("${app.privacy.threshold.sitesResult}")
  private long thresholdSitesResult;

  @Value("${app.maxSavedQueriesPerUser}")
  private long maxSavedQueriesPerUser;

  @NotNull
  private static Ccdl createValidCcdl() {
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
        .exclusionCriteria(List.of())
        .build();
  }

  @NotNull
  private static Ccdl createValidAnnotatedCcdl(boolean withIssues) {
    var termCode = TermCode.builder()
        .code("LL2191-6")
        .system("http://loinc.org")
        .display("Geschlecht")
        .build();
    var inclusionCriterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .attributeFilters(List.of())
        .context(termCode)
        .validationIssueTypes(withIssues ? List.of(ValidationIssueType.TERMCODE_CONTEXT_COMBINATION_INVALID) : List.of())
        .build();
    return Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(inclusionCriterion)))
        .exclusionCriteria(List.of())
        .display("foo")
        .build();
  }

  @NotNull
  private static de.medizininformatikinitiative.dataportal.backend.query.persistence.Query createValidQuery(long id) {
    var query = new de.medizininformatikinitiative.dataportal.backend.query.persistence.Query();
    query.setId(id);
    query.setCreatedAt(new Timestamp(new java.util.Date().getTime()));
    query.setCreatedBy("someone");
    query.setQueryContent(createValidQueryContent(id));
    return query;
  }

  @NotNull
  private static de.medizininformatikinitiative.dataportal.backend.query.persistence.QueryContent createValidQueryContent(long id) {
    var queryContent = new de.medizininformatikinitiative.dataportal.backend.query.persistence.QueryContent();
    queryContent.setId(id);
    queryContent.setQueryContent(createValidCcdl().toString());
    queryContent.setHash("abc");
    return queryContent;
  }

  @NotNull
  private static QueryListEntry createValidQueryListEntry(long id, boolean skipValidation) {
    if (skipValidation) {
      return QueryListEntry.builder()
          .id(id)
          .label("abc")
          .createdAt(new Timestamp(new java.util.Date().getTime()))
          .build();
    } else {
      return QueryListEntry.builder()
          .id(id)
          .label("abc")
          .createdAt(new Timestamp(new java.util.Date().getTime()))
          .isValid(true)
          .build();
    }
  }

  @NotNull
  private static TermCode createTermCode() {
    return TermCode.builder()
        .code("LL2191-6")
        .system("http://loinc.org")
        .display("Geschlecht")
        .build();
  }

  @NotNull
  private static Criterion createInvalidCriterion() {
    return Criterion.builder()
        .termCodes(List.of(createTermCode()))
        .context(null)
        .build();
  }

  @NotNull
  private static Query createValidApiQuery(long id) {
    return Query.builder()
        .id(id)
        .content(createValidCcdl())
        .label("test")
        .comment("test")
        .build();
  }

  @NotNull
  private static QueryResult createTestQueryResult(QueryHandlerService.ResultDetail resultDetail) {
    List<QueryResultLine> queryResultLines;
    long totalNumberOfPatients;

    if (resultDetail == QueryHandlerService.ResultDetail.SUMMARY) {
      queryResultLines = List.of();
      totalNumberOfPatients = 999L;
    } else {
      var resultLines = List.of(
          ResultLine.builder()
              .siteName("A")
              .type(SUCCESS)
              .result(123L)
              .build(),
          ResultLine.builder()
              .siteName("B")
              .type(SUCCESS)
              .result(456L)
              .build(),
          ResultLine.builder()
              .siteName("C")
              .type(SUCCESS)
              .result(789L)
              .build()
      );
      queryResultLines = resultLines.stream()
          .map(ssr -> QueryResultLine.builder()
              .siteName(resultDetail == QueryHandlerService.ResultDetail.DETAILED_OBFUSCATED ? "foobar" + ssr.siteName()
                  : ssr.siteName())
              .numberOfPatients(ssr.result())
              .build())
          .toList();

      totalNumberOfPatients = queryResultLines.stream().map(QueryResultLine::numberOfPatients).reduce(0L, Long::sum);
    }

    return QueryResult.builder()
        .queryId(1L)
        .totalNumberOfPatients(totalNumberOfPatients)
        .resultLines(queryResultLines)
        .build();
  }

  @NotNull
  private static QueryResult createTestDetailedObfuscatedQueryResultWithTooFewResults(long threshold) {
    List<QueryResultLine> queryResultLines;


    var resultLines = List.of(
        ResultLine.builder()
            .siteName("A")
            .type(SUCCESS)
            .result(threshold - 1)
            .build(),
        ResultLine.builder()
            .siteName("B")
            .type(SUCCESS)
            .result(threshold - 2)
            .build(),
        ResultLine.builder()
            .siteName("C")
            .type(SUCCESS)
            .result(threshold - 1)
            .build()
    );
    queryResultLines = resultLines.stream()
        .map(ssr -> QueryResultLine.builder()
            .siteName("foobar" + ssr.siteName())
            .numberOfPatients(ssr.result())
            .build())
        .toList();

    return QueryResult.builder()
        .queryId(1L)
        .totalNumberOfPatients(queryResultLines.stream().map(QueryResultLine::numberOfPatients).reduce(0L, Long::sum))
        .resultLines(queryResultLines)
        .build();
  }

  @NotNull
  private static QueryQuota createDummyQueryQuota() {
    return QueryQuota.builder()
        .hard(
            QueryQuotaEntry.builder()
                .used(5)
                .limit(10)
                .interval("PT100M")
                .build()
        )
        .soft(
            QueryQuotaEntry.builder()
                .used(20)
                .limit(50)
                .interval("PT1000M")
                .build()
        )
        .build();
  }

  @BeforeEach
  void initTest() throws Exception {
    when(rateLimitingInterceptor.preHandle(any(), any(), any())).thenReturn(true);
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  public void testRunQueryEndpoint_FailsOnInvalidCcdlWith400() throws Exception {
    var testQuery = Ccdl.builder().build();

    doReturn(List.of(ValidationIssue.builder()
        .path("foo")
        .value(ValidationIssueValue.builder()
            .code("VAL-000")
            .message("bar")
            .build())
        .build()))
        .when(queryHandlerService).validateCcdl(any(JsonNode.class));

    var mvcResult = mockMvc.perform(post(URI.create(PATH)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(testQuery)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER", username = "test")
  public void testRunQueryEndpoint_SucceedsOnValidCcdlWith201() throws Exception {
    Ccdl testQuery = createValidCcdl();
    var annotatedQuery = createValidAnnotatedCcdl(false);

    doReturn(List.of()).when(queryHandlerService).validateCcdl(any(JsonNode.class));
    doReturn(testQuery).when(queryHandlerService).ccdlFromJsonNode(any(JsonNode.class));
    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    doReturn(createValidUiProfileString()).when(terminologyService).getUiProfile(any(String.class));
    doReturn(Mono.just(1L)).when(queryHandlerService).runQuery(any(Ccdl.class), eq("test"));
    doReturn(annotatedQuery).when(ccdlValidation).annotateCcdl(any(Ccdl.class), any(Boolean.class));

    mockMvc.perform(post(URI.create(PATH)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(testQuery)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("location"))
        .andExpect(header().string("location", PATH + "/1"));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER", username = "test")
  public void testRunQueryEndpoint_FailsOnDownstreamServiceError() throws Exception {
    Ccdl testQuery = createValidCcdl();
    var annotatedQuery = createValidAnnotatedCcdl(false);

    var dispatchError = new QueryDispatchException("something went wrong");

    doReturn(List.of()).when(queryHandlerService).validateCcdl(any(JsonNode.class));
    doReturn(testQuery).when(queryHandlerService).ccdlFromJsonNode(any(JsonNode.class));
    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    doReturn(createValidUiProfileString()).when(terminologyService).getUiProfile(any(String.class));
    doReturn(Mono.error(dispatchError)).when(queryHandlerService).runQuery(any(Ccdl.class), eq("test"));
    doReturn(annotatedQuery).when(ccdlValidation).annotateCcdl(any(Ccdl.class), any(Boolean.class));

    mockMvc.perform(post(URI.create(PATH)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(testQuery)))
        .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER", username = "test")
  public void testRunQueryEndpoint_FailsOnSoftQuotaExceeded() throws Exception {
    Ccdl testQuery = createValidCcdl();
    var annotatedQuery = createValidAnnotatedCcdl(false);

    doReturn(List.of()).when(queryHandlerService).validateCcdl(any(JsonNode.class));
    doReturn(testQuery).when(queryHandlerService).ccdlFromJsonNode(any(JsonNode.class));
    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    doReturn(createValidUiProfileString()).when(terminologyService).getUiProfile(any(String.class));
    doReturn((long) quotaSoftCreateAmount + 1).when(queryHandlerService).getAmountOfQueriesByUserAndInterval(any(String.class), any(String.class));
    doReturn(annotatedQuery).when(ccdlValidation).annotateCcdl(any(Ccdl.class), any(Boolean.class));

    mockMvc.perform(post(URI.create(PATH)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(testQuery)))
        .andExpect(status().is(HttpStatus.TOO_MANY_REQUESTS.value()));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER", username = "test")
  public void testValidateQueryEndpoint_SucceedsOnValidQuery() throws Exception {
    Ccdl testQuery = createValidCcdl();
    var annotatedQuery = createValidAnnotatedCcdl(false);

    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    doReturn(createValidUiProfileString()).when(terminologyService).getUiProfile(any(String.class));

    doReturn(List.of()).when(queryHandlerService).validateCcdl(any(JsonNode.class));
    doReturn(testQuery).when(queryHandlerService).ccdlFromJsonNode(any(JsonNode.class));
    doReturn(annotatedQuery).when(ccdlValidation).annotateCcdl(any(Ccdl.class), any(Boolean.class));

    mockMvc.perform(post(URI.create(PATH + "/validate")).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(testQuery)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inclusionCriteria[0].[0].issues").isArray())
        .andExpect(jsonPath("$.inclusionCriteria[0].[0].issues").isEmpty());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  public void testValidateQueryEndpoint_FailsOnInvalidCriteriaWith400() throws Exception {
    Ccdl testQuery = createValidCcdl();
    var annotatedQuery = createValidAnnotatedCcdl(true);

    doReturn(List.of(Error.builder().message("error").instanceLocation(new NodePath(PathType.DEFAULT)).build())).when(queryHandlerService).validateCcdl(any(JsonNode.class));
    doReturn(testQuery).when(queryHandlerService).ccdlFromJsonNode(any(JsonNode.class));
    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    doReturn(createValidUiProfileString()).when(terminologyService).getUiProfile(any(String.class));
    doReturn(annotatedQuery).when(ccdlValidation).annotateCcdl(any(Ccdl.class), any(Boolean.class));

    mockMvc.perform(post(URI.create(PATH + "/validate")).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(testQuery)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER", username = "test")
  public void testRunQueryEndpoint_FailsOnBeingBlacklistedWith403() throws Exception {
    Ccdl testQuery = createValidCcdl();
    var annotatedQuery = createValidAnnotatedCcdl(false);
    UserBlacklist userBlacklistEntry = new UserBlacklist();
    userBlacklistEntry.setId(1L);
    userBlacklistEntry.setUserId("test");
    userBlacklistEntry.setBlacklistedAt(new Timestamp(System.currentTimeMillis()));
    Optional<UserBlacklist> userBlacklistOptional = Optional.of(userBlacklistEntry);

    doReturn(List.of()).when(queryHandlerService).validateCcdl(any(JsonNode.class));
    doReturn(testQuery).when(queryHandlerService).ccdlFromJsonNode(any(JsonNode.class));
    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    doReturn(createValidUiProfileString()).when(terminologyService).getUiProfile(any(String.class));
    doReturn(userBlacklistOptional).when(userBlacklistRepository).findByUserId(any(String.class));
    doReturn(Mono.just(1L)).when(queryHandlerService).runQuery(any(Ccdl.class), eq("test"));
    doReturn(annotatedQuery).when(ccdlValidation).annotateCcdl(any(Ccdl.class), any(Boolean.class));

    mockMvc.perform(post(URI.create(PATH)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(testQuery)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER", username = "test")
  public void testRunQueryEndpoint_FailsOnExceedingHardLimitWith403() throws Exception {
    Ccdl testQuery = createValidCcdl();

    doReturn(List.of()).when(queryHandlerService).validateCcdl(any(JsonNode.class));
    doReturn(testQuery).when(queryHandlerService).ccdlFromJsonNode(any(JsonNode.class));
    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    doReturn(createValidUiProfileString()).when(terminologyService).getUiProfile(any(String.class));
    doReturn((long) quotaHardCreateAmount).when(queryHandlerService).getAmountOfQueriesByUserAndInterval(any(String.class), eq(quotaHardCreateInterval));
    doReturn(Mono.just(1L)).when(queryHandlerService).runQuery(any(Ccdl.class), eq("test"));

    mockMvc.perform(post(URI.create(PATH)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(testQuery)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"DATAPORTAL_TEST_USER", "DATAPORTAL_TEST_POWER"}, username = "test")
  public void testRunQueryEndpoint_SucceedsOnExceedingHardlimitAsPowerUserWith201() throws Exception {
    Ccdl testQuery = createValidCcdl();
    var annotatedQuery = createValidAnnotatedCcdl(false);


    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    doReturn(createValidUiProfileString()).when(terminologyService).getUiProfile(any(String.class));

    doReturn(List.of()).when(queryHandlerService).validateCcdl(any(JsonNode.class));
    doReturn(testQuery).when(queryHandlerService).ccdlFromJsonNode(any(JsonNode.class));
    doReturn(true).when(authenticationHelper).hasAuthority(any(Authentication.class), eq("ROLE_DATAPORTAL_TEST_POWER"));
    doReturn((long) quotaHardCreateAmount).when(queryHandlerService).getAmountOfQueriesByUserAndInterval(any(String.class), eq(quotaHardCreateInterval));
    doReturn((long) (quotaSoftCreateAmount - 1)).when(queryHandlerService).getAmountOfQueriesByUserAndInterval(any(String.class), eq(quotaSoftCreateInterval));
    doReturn(Mono.just(1L)).when(queryHandlerService).runQuery(any(Ccdl.class), eq("test"));
    doReturn(annotatedQuery).when(ccdlValidation).annotateCcdl(any(Ccdl.class), any(Boolean.class));

    mockMvc.perform(post(URI.create(PATH)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(testQuery)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("location"))
        .andExpect(header().string("location", PATH + "/1"));
  }

  @ParameterizedTest
  @EnumSource
  @WithMockUser(roles = {"DATAPORTAL_TEST_ADMIN"}, username = "test")
  public void testGetQueryResult_succeeds(QueryHandlerService.ResultDetail resultDetail) throws Exception {
    var requestUri = PATH + "/1";
    doReturn(true).when(authenticationHelper).hasAuthority(any(Authentication.class), eq("ROLE_DATAPORTAL_TEST_ADMIN"));
    doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
    doReturn(createTestQueryResult(resultDetail)).when(queryHandlerService).getQueryResult(any(Long.class), any(QueryHandlerService.ResultDetail.class));

    switch (resultDetail) {
      case SUMMARY -> requestUri = requestUri + WebSecurityConfig.PATH_SUMMARY_RESULT;
      case DETAILED_OBFUSCATED -> requestUri = requestUri + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT;
      case DETAILED -> requestUri = requestUri + WebSecurityConfig.PATH_DETAILED_RESULT;
    }

    switch (resultDetail) {
      case SUMMARY -> mockMvc.perform(get(requestUri).with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalNumberOfPatients").exists())
          .andExpect(jsonPath("$.resultLines", empty()));
      case DETAILED_OBFUSCATED -> mockMvc.perform(get(requestUri).with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalNumberOfPatients").exists())
          .andExpect(jsonPath("$.resultLines").exists())
          .andExpect(jsonPath("$.resultLines[0].siteName", startsWith("foobar")));
      case DETAILED -> mockMvc.perform(get(requestUri).with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalNumberOfPatients").exists())
          .andExpect(jsonPath("$.resultLines").exists())
          .andExpect(jsonPath("$.resultLines[0].siteName", not(startsWith("foobar"))));
    }
  }

  @Test
  @WithMockUser(roles = {"DATAPORTAL_TEST_USER"}, username = "test")
  public void testGetDetailedObfuscatedQueryResult_returnsIssueWhenBelowThreshold() throws Exception {
    var requestUri = PATH + "/1" + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT;
    doReturn(true).when(authenticationHelper).hasAuthority(any(Authentication.class), eq("ROLE_DATAPORTAL_TEST_USER"));
    doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
    doReturn(createTestDetailedObfuscatedQueryResultWithTooFewResults(thresholdSitesResult))
        .when(queryHandlerService).getQueryResult(any(Long.class), any(QueryHandlerService.ResultDetail.class));


    mockMvc.perform(get(requestUri).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resultLines").doesNotExist())
        .andExpect(jsonPath("$.issues").exists())
        .andExpect(jsonPath("$.issues").isArray())
        .andExpect(jsonPath("$.issues[0].code").value("FEAS-" + FeasibilityIssue.PRIVACY_RESTRICTION_RESULT_SITES.code()));
  }

  @Test
  @WithMockUser(roles = {"DATAPORTAL_TEST_USER"}, username = "test")
  public void testGetDetailedObfuscatedResult_failsOnWrongAuthorWith403() throws Exception {
    doReturn("some-other-user").when(queryHandlerService).getAuthorId(any(Long.class));

    mockMvc.perform(get(URI.create(PATH + "/1" + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"DATAPORTAL_TEST_USER"}, username = "test")
  public void testGetDetailedObfuscatedResultRateLimit_succeeds() throws Exception {
    mockMvc.perform(get(URI.create(PATH + "/detailed-obfuscated-result-rate-limit")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.limit").exists())
        .andExpect(jsonPath("$.remaining").exists());
  }

  @Test
  public void testGetDetailedObfuscatedResultRateLimit_failsOnNotLoggedIn() throws Exception {
    mockMvc.perform(get(URI.create(PATH + "/detailed-obfuscated-result-rate-limit")).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = {"DATAPORTAL_TEST_USER"}, username = "test")
  public void testGetQueryQuota() throws Exception {
    doReturn(createDummyQueryQuota()).when(queryHandlerService).getSentQueryStatistics(any(String.class), anyInt(), anyString(), anyInt(), anyString());

    mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_FEASIBILITY + "/quota")).with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = {"DATAPORTAL_TEST_USER"}, username = "test")
  public void testSq2Cql_succeeds() throws Exception {
    doReturn(List.of()).when(queryHandlerService).validateCcdl(any(JsonNode.class));
    doReturn(createValidCcdl()).when(queryHandlerService).ccdlFromJsonNode(any(JsonNode.class));
    doReturn(createDummyCql()).when(queryHandlerService).translateQueryToCql(any(Ccdl.class));

    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    doReturn(createValidUiProfileString()).when(terminologyService).getUiProfile(any(String.class));

    mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + PATH_FEASIBILITY + "/cql")).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(createValidCcdl())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/cql;charset=UTF-8"));
  }

  @Test
  @WithMockUser(roles = {"DATAPORTAL_TEST_USER"}, username = "test")
  public void testSq2Cql_failsWith400() throws Exception {
    doReturn(List.of(Error.builder().message("error").instanceLocation(new NodePath(PathType.DEFAULT)).build())).when(queryHandlerService).validateCcdl(any(JsonNode.class));
    doReturn(createValidCcdl()).when(queryHandlerService).ccdlFromJsonNode(any(JsonNode.class));
    doThrow(QueryTranslationException.class).when(queryHandlerService).translateQueryToCql(any(Ccdl.class));

    mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + PATH_FEASIBILITY + "/cql")).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(createValidCcdl())))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = {"DATAPORTAL_TEST_USER"}, username = "test")
  public void testSq2Cql_failsWith422() throws Exception {
    doReturn(List.of()).when(queryHandlerService).validateCcdl(any(JsonNode.class));
    doReturn(createValidCcdl()).when(queryHandlerService).ccdlFromJsonNode(any(JsonNode.class));
    doThrow(QueryTranslationException.class).when(queryHandlerService).translateQueryToCql(any(Ccdl.class));

    doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class));
    doReturn(createValidUiProfileString()).when(terminologyService).getUiProfile(any(String.class));

    mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + PATH_FEASIBILITY + "/cql")).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(createValidCcdl())))
        .andExpect(status().isUnprocessableEntity());
  }

  @NotNull
  private String createDummyCql() {
    return """
        library Retrieve version '1.0.0'
        using FHIR version '4.0.0'
        include FHIRHelpers version '4.0.0'
        
        context Patient
        
        define Criterion:
          AgeInYears() >= 50.0
        
        define InInitialPopulation:
          Criterion
        
        """;
  }

  @NotNull
  private String createValidUiProfileString() {
    return """
        {
            "attributeDefinitions": [],
            "name": "Patient1",
            "timeRestrictionAllowed": false,
            "valueDefinition": {
                "allowedUnits": [],
                "display": {
                    "original": "Geschlecht",
                    "translations": [
                        {
                            "language": "en-US",
                            "value": "Gender"
                        },
                        {
                            "language": "de-DE",
                            "value": "Geschlecht"
                        }
                    ]
                },
                "max": null,
                "min": null,
                "optional": false,
                "precision": 1,
                "referencedCriteriaSet": [],
                "referencedValueSet": [
                    "http://hl7.org/fhir/ValueSet/administrative-gender"
                ],
                "type": "concept"
            }
        }
        """;
  }
}
