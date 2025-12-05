package de.medizininformatikinitiative.dataportal.backend.config;

import de.medizininformatikinitiative.dataportal.backend.common.api.Criterion;
import de.medizininformatikinitiative.dataportal.backend.common.api.TermCode;
import de.medizininformatikinitiative.dataportal.backend.dse.DseService;
import de.medizininformatikinitiative.dataportal.backend.query.QueryHandlerService;
import de.medizininformatikinitiative.dataportal.backend.query.api.Crtdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.Dataquery;
import de.medizininformatikinitiative.dataportal.backend.query.api.StructuredQuery;
import de.medizininformatikinitiative.dataportal.backend.query.dataquery.DataqueryHandler;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.UserBlacklistRepository;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.AuthenticationHelper;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.medizininformatikinitiative.dataportal.backend.query.v5.DataqueryHandlerRestController;
import de.medizininformatikinitiative.dataportal.backend.terminology.TerminologyService;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.CodeableConceptService;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.TerminologyEsService;
import de.medizininformatikinitiative.dataportal.backend.terminology.v5.TerminologyRestController;
import de.medizininformatikinitiative.dataportal.backend.terminology.validation.StructuredQueryValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(SpringExtension.class)
@Import({AuthenticationHelper.class,
    RateLimitingServiceSpringConfig.class,
    WebSecurityConfig.class
})
@WebMvcTest(
    controllers = {DataqueryHandlerRestController.class,
        TerminologyRestController.class
    }
)
class WebSecurityConfigIT {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DseService dseService;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private DataqueryHandler dataqueryHandler;

  @MockitoBean
  private StructuredQueryValidation structuredQueryValidation;

  @MockitoBean
  private QueryHandlerService queryHandlerService;

  @MockitoBean
  private UserBlacklistRepository userBlacklistRepository;

  @MockitoBean
  private CodeableConceptService codeableConceptService;

  @MockitoBean
  private TerminologyService terminologyService;

  @MockitoBean
  private TerminologyEsService terminologyEsService;

  // Get criteria profile data as example to check if the matcher for /terminology/** correctly works
  @Test
  @WithMockUser(username = "user", roles = "DATAPORTAL_TEST_USER")
  void getCriteriaProfileData_authenticatedAllowed() throws Exception {
    mockMvc.perform(get(URI.create("/api/v5/terminology/criteria-profile-data")).with(csrf())
            .param("ids", "")
        )
        .andExpect(status().isOk());
  }

  @Test
  void getCriteriaProfileData_unauthenticatedDenied() throws Exception {
    mockMvc.perform(get(URI.create("/api/v5/terminology/criteria-profile-data")).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "user", roles = "DATAPORTAL_TEST_ADMIN")
  void getCriteriaProfileData_adminAccessDenied() throws Exception {
    mockMvc.perform(get(URI.create("/api/v5/terminology/criteria-profile-data")).with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "user", roles = "DATAPORTAL_TEST_USER")
  void getDataqueryList_authenticatedAllowed() throws Exception {
    mockMvc.perform(get(URI.create("/api/v5/query/data")).with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void getDataqueryList_unauthenticatedDenied() throws Exception {
    mockMvc.perform(get(URI.create("/api/v5/query/data")).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "user", roles = "DATAPORTAL_TEST_ADMIN")
  void getDataqueryList_adminAccessDenied() throws Exception {
    mockMvc.perform(get(URI.create("/api/v5/query/data")).with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "user", roles = "DATAPORTAL_TEST_USER")
  void getDataqueryListByUser_regularUserDenied() throws Exception {
    mockMvc.perform(get(URI.create("/api/v5/query/data/by-user/abc-123")).with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void getDataqueryListByUser_unauthenticatedDenied() throws Exception {
    mockMvc.perform(get(URI.create("/api/v5/query/data/by-user/abc-123")).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "user", roles = "DATAPORTAL_TEST_ADMIN")
  void getDataqueryListByUser_adminAccessAllowed() throws Exception {
    mockMvc.perform(get(URI.create("/api/v5/query/data/by-user/abc-123")).with(csrf()))
        .andExpect(status().isOk());
  }

  // Check if admin and regular user can access a query by id. If it is 404 the endpoint can be reached (not forbidden/unauthorized)
  @Test
  @WithMockUser(username = "user", roles = "DATAPORTAL_TEST_USER")
  void getDataquery_authenticatedAllowed() throws Exception {
    doReturn(createDataquery(false)).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));

    mockMvc.perform(get(URI.create("/api/v5/query/data/1")).with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void getDataquery_unauthenticatedDenied() throws Exception {
    doReturn(createDataquery(false)).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));
    mockMvc.perform(get(URI.create("/api/v5/query/data/1")).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "user", roles = "DATAPORTAL_TEST_ADMIN")
  void getDataquery_adminAccessAllowed() throws Exception {
    doReturn(createDataquery(false)).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));
    mockMvc.perform(get(URI.create("/api/v5/query/data/1")).with(csrf()))
        .andExpect(status().isOk());
  }

  private Dataquery createDataquery(boolean withResult) {
    return Dataquery.builder()
        .label("test")
        .comment("test")
        .content(createCrtdl())
        .resultSize(withResult ? 123L : null)
        .build();
  }

  private Crtdl createCrtdl() {
    return Crtdl.builder()
        .cohortDefinition(createValidStructuredQuery())
        .display("foo")
        .build();
  }

  private StructuredQuery createValidStructuredQuery() {
    var termCode = TermCode.builder()
        .code("LL2191-6")
        .system("http://loinc.org")
        .display("Geschlecht")
        .build();
    var inclusionCriterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .attributeFilters(List.of())
        .build();
    return StructuredQuery.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(inclusionCriterion)))
        .exclusionCriteria(null)
        .display("foo")
        .build();
  }

}
