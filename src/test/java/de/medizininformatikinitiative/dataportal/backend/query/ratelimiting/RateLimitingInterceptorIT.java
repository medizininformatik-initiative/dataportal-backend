package de.medizininformatikinitiative.dataportal.backend.query.ratelimiting;

import de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig;
import de.medizininformatikinitiative.dataportal.backend.query.QueryHandlerService;
import de.medizininformatikinitiative.dataportal.backend.query.QueryHandlerService.ResultDetail;
import de.medizininformatikinitiative.dataportal.backend.query.api.QueryResult;
import de.medizininformatikinitiative.dataportal.backend.query.api.QueryResultLine;
import de.medizininformatikinitiative.dataportal.backend.query.api.validation.CcdlValidatorSpringConfig;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.UserBlacklistRepository;
import de.medizininformatikinitiative.dataportal.backend.query.result.ResultLine;
import de.medizininformatikinitiative.dataportal.backend.query.v5.FeasibilityQueryHandlerRestController;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.*;
import static de.medizininformatikinitiative.dataportal.backend.query.persistence.ResultType.SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("query")
@Tag("rate-limiting")
@ExtendWith(SpringExtension.class)
@Import({CcdlValidatorSpringConfig.class,
    RateLimitingServiceSpringConfig.class
})
@WebMvcTest(
    controllers = FeasibilityQueryHandlerRestController.class,
    properties = {
        "app.enableQueryValidation=true",
        "app.privacy.quota.read.resultSummary.pollingInterval=PT1S",
        "app.privacy.quota.read.detailedObfuscated.pollingInterval=PT2S",
        "app.privacy.quota.read.detailedObfuscated.amount=1",
        "app.privacy.quota.read.detailedObfuscated.interval=PT3S"
    }
)
@SuppressWarnings("NewClassNamingConvention")
public class RateLimitingInterceptorIT {

  @MockitoBean
  AuthenticationHelper authenticationHelper;
  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private QueryHandlerService queryHandlerService;
  @MockitoBean
  private CcdlValidation ccdlValidation;
  @MockitoBean
  private UserBlacklistRepository userBlacklistRepository;

  @MockitoBean
  private TerminologyEsService terminologyEsService;

  @MockitoBean
  private TerminologyService terminologyService;

  @MockitoBean
  private CodeableConceptService codeableConceptService;

  @NotNull
  private static QueryResult createTestQueryResult(ResultDetail resultDetail) {
    List<QueryResultLine> queryResultLines;

    if (resultDetail == ResultDetail.SUMMARY) {
      queryResultLines = List.of();
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
              .siteName(resultDetail == ResultDetail.DETAILED_OBFUSCATED ? "foobar" + ssr.siteName()
                  : ssr.siteName())
              .numberOfPatients(ssr.result())
              .build())
          .toList();
    }

    return QueryResult.builder()
        .queryId(1L)
        .totalNumberOfPatients(123L)
        .resultLines(queryResultLines)
        .build();
  }

  @BeforeEach
  void setupMockBehaviour() throws InvalidAuthenticationException {
    doReturn(true).when(authenticationHelper)
        .hasAuthority(any(Authentication.class), eq("ROLE_DATAPORTAL_TEST_USER"));
    doReturn(createTestQueryResult(ResultDetail.SUMMARY)).when(queryHandlerService)
        .getQueryResult(any(Long.class), eq(ResultDetail.SUMMARY));
    doReturn(createTestQueryResult(ResultDetail.DETAILED)).when(queryHandlerService)
        .getQueryResult(any(Long.class), eq(ResultDetail.DETAILED));
    doReturn(createTestQueryResult(ResultDetail.DETAILED_OBFUSCATED)).when(queryHandlerService)
        .getQueryResult(any(Long.class), eq(ResultDetail.DETAILED_OBFUSCATED));
  }

  @ParameterizedTest
  @EnumSource
  public void testGetResult_SucceedsOnFirstCall(ResultDetail resultDetail) throws Exception {
    var authorName = UUID.randomUUID().toString();
    var requestUri = PATH_API + PATH_QUERY + PATH_FEASIBILITY + "/1";
    boolean isAdmin = false;

    switch (resultDetail) {
      case SUMMARY -> requestUri = requestUri + WebSecurityConfig.PATH_SUMMARY_RESULT;
      case DETAILED_OBFUSCATED -> requestUri = requestUri + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT;
      case DETAILED -> {
        requestUri = requestUri + WebSecurityConfig.PATH_DETAILED_RESULT;
        isAdmin = true;
      }
    }

    doReturn(isAdmin).when(authenticationHelper)
        .hasAuthority(any(Authentication.class), eq("ROLE_DATAPORTAL_TEST_ADMIN"));
    doReturn(authorName).when(queryHandlerService).getAuthorId(any(Long.class));

    mockMvc
        .perform(
            get(URI.create(requestUri)).with(csrf())
                .with(user(authorName).password("pass").roles("DATAPORTAL_TEST_USER"))
        )
        .andExpect(status().isOk());
  }

  @ParameterizedTest
  @EnumSource
  public void testGetResult_FailsOnImmediateSecondCall(ResultDetail resultDetail) throws Exception {
    var authorName = UUID.randomUUID().toString();
    var requestUri = PATH_API + PATH_QUERY + PATH_FEASIBILITY + "/1";

    switch (resultDetail) {
      case SUMMARY -> requestUri = requestUri + WebSecurityConfig.PATH_SUMMARY_RESULT;
      case DETAILED_OBFUSCATED -> requestUri = requestUri + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT;
      case DETAILED -> {
        // This endpoint is only available for admin users, which are not affected by rate limiting
        return;
      }
    }

    doReturn(false).when(authenticationHelper)
        .hasAuthority(any(Authentication.class), eq("ROLE_DATAPORTAL_TEST_ADMIN"));
    doReturn(authorName).when(queryHandlerService).getAuthorId(any(Long.class));

    mockMvc
        .perform(
            get(requestUri).with(csrf())
                .with(user(authorName).password("pass").roles("DATAPORTAL_TEST_USER"))
        )
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get(URI.create(requestUri)).with(csrf())
                .with(user(authorName).password("pass").roles("DATAPORTAL_TEST_USER"))
        )
        .andExpect(status().isTooManyRequests());
  }

  @ParameterizedTest
  @EnumSource
  public void testGetResult_SucceedsOnDelayedSecondCall(ResultDetail resultDetail) throws Exception {
    var authorName = UUID.randomUUID().toString();
    var requestUri = PATH_API + PATH_QUERY + PATH_FEASIBILITY + "/1";

    switch (resultDetail) {
      case SUMMARY -> requestUri = requestUri + WebSecurityConfig.PATH_SUMMARY_RESULT;
      case DETAILED_OBFUSCATED -> requestUri = requestUri + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT;
      case DETAILED -> {
        // This endpoint is only available for admin users, which are not affected by rate limiting
        return;
      }
    }

    doReturn(false).when(authenticationHelper)
        .hasAuthority(any(Authentication.class), eq("ROLE_DATAPORTAL_TEST_ADMIN"));
    doReturn(authorName).when(queryHandlerService).getAuthorId(any(Long.class));

    mockMvc
        .perform(
            get(requestUri).with(csrf())
                .with(user(authorName).password("pass").roles("DATAPORTAL_TEST_USER"))
        )
        .andExpect(status().isOk());

    Thread.sleep(1001L);

    mockMvc
        .perform(
            get(URI.create(PATH_API + PATH_QUERY + PATH_FEASIBILITY + "/1" + WebSecurityConfig.PATH_SUMMARY_RESULT)).with(csrf())
                .with(user(authorName).password("pass").roles("DATAPORTAL_TEST_USER"))
        )
        .andExpect(status().isOk());
  }

  @ParameterizedTest
  @EnumSource
  public void testGetResult_SucceedsOnImmediateMultipleCallsAsAdmin(ResultDetail resultDetail)
      throws Exception {

    var authorName = UUID.randomUUID().toString();
    var requestUri = PATH_API + PATH_QUERY + PATH_FEASIBILITY + "/1";

    switch (resultDetail) {
      case SUMMARY -> requestUri = requestUri + WebSecurityConfig.PATH_SUMMARY_RESULT;
      case DETAILED_OBFUSCATED -> requestUri = requestUri + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT;
      case DETAILED -> requestUri = requestUri + WebSecurityConfig.PATH_DETAILED_RESULT;
    }

    doReturn(true).when(authenticationHelper)
        .hasAuthority(any(Authentication.class), eq("ROLE_DATAPORTAL_TEST_ADMIN"));
    doReturn(authorName).when(queryHandlerService).getAuthorId(any(Long.class));

    for (int i = 0; i < 10; ++i) {
      mockMvc
          .perform(
              get(requestUri).with(csrf())
                  .with(user(authorName).password("pass").roles("DATAPORTAL_TEST_ADMIN"))
          )
          .andExpect(status().isOk());
    }
  }

  @ParameterizedTest
  @EnumSource
  public void testGetResult_SucceedsOnImmediateSecondCallAsOtherUser(ResultDetail resultDetail)
      throws Exception {
    var authorName = UUID.randomUUID().toString();
    var requestUri = PATH_API + PATH_QUERY + PATH_FEASIBILITY + "/1";

    switch (resultDetail) {
      case SUMMARY -> requestUri = requestUri + WebSecurityConfig.PATH_SUMMARY_RESULT;
      case DETAILED_OBFUSCATED -> requestUri = requestUri + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT;
      case DETAILED -> {
        // This endpoint is only available for admin users, which are not affected by rate limiting
        return;
      }
    }

    doReturn(false).when(authenticationHelper)
        .hasAuthority(any(Authentication.class), eq("ROLE_DATAPORTAL_TEST_ADMIN"));
    doReturn(authorName).when(queryHandlerService).getAuthorId(any(Long.class));

    mockMvc
        .perform(
            get(requestUri).with(csrf())
                .with(user(authorName).password("pass").roles("DATAPORTAL_TEST_USER"))
        )
        .andExpect(status().isOk());

    authorName = UUID.randomUUID().toString();
    doReturn(authorName).when(queryHandlerService).getAuthorId(any(Long.class));

    mockMvc
        .perform(
            get(URI.create(PATH_API + PATH_QUERY + PATH_FEASIBILITY + "/1" + WebSecurityConfig.PATH_SUMMARY_RESULT)).with(csrf())
                .with(user(authorName).password("pass").roles("DATAPORTAL_TEST_USER"))
        )
        .andExpect(status().isOk());
  }

  @Test
  public void testGetDetailedObfuscatedResult_FailsOnLimitExceedingCall() throws Exception {
    var authorName = UUID.randomUUID().toString();
    var requestUri = PATH_API + PATH_QUERY + PATH_FEASIBILITY + "/1" + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT;

    doReturn(false).when(authenticationHelper)
        .hasAuthority(any(Authentication.class), eq("ROLE_DATAPORTAL_TEST_ADMIN"));
    doReturn(authorName).when(queryHandlerService).getAuthorId(any(Long.class));

    mockMvc
        .perform(
            get(requestUri).with(csrf())
                .with(user(authorName).password("pass").roles("DATAPORTAL_TEST_USER"))
        )
        .andExpect(status().isOk());

    // Wait longer than 1 second to avoid running into the general rate limit
    Thread.sleep(1001L);

    mockMvc
        .perform(
            get(URI.create(requestUri)).with(csrf())
                .with(user(authorName).password("pass").roles("DATAPORTAL_TEST_USER"))
        )
        .andExpect(status().isTooManyRequests());

    Thread.sleep(2001L);

    mockMvc
        .perform(
            get(requestUri).with(csrf())
                .with(user(authorName).password("pass").roles("DATAPORTAL_TEST_USER"))
        )
        .andExpect(status().isOk());
  }

}
