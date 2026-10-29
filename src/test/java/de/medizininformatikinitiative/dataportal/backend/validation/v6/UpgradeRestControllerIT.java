package de.medizininformatikinitiative.dataportal.backend.validation.v6;

import de.medizininformatikinitiative.dataportal.backend.dse.DseService;
import de.medizininformatikinitiative.dataportal.backend.dse.api.DseProfile;
import de.medizininformatikinitiative.dataportal.backend.dse.api.Field;
import de.medizininformatikinitiative.dataportal.backend.dse.api.Filter;
import de.medizininformatikinitiative.dataportal.backend.query.api.Crtdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.UpgradeIssueType;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssueType;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssueValue;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssue;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.AuthenticationHelper;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.medizininformatikinitiative.dataportal.backend.validation.UpgradeService;
import de.medizininformatikinitiative.dataportal.backend.validation.ValidationService;
import de.medizininformatikinitiative.dataportal.backend.validation.api.UpgradeWrapper;
import org.hl7.fhir.utilities.tests.TestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("query")
@Tag("validation")
@ExtendWith(SpringExtension.class)
@Import({RateLimitingServiceSpringConfig.class, TestConfig.class})
@WebMvcTest(controllers = UpgradeRestController.class)
class UpgradeRestControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper jsonUtil;

  private static final String DIAGNOSE_PROFILE_URL =
      "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose";

  @MockitoBean
  private ValidationService validationService;

  @MockitoBean
  private UpgradeService upgradeService;

  @MockitoBean
  private AuthenticationHelper authenticationHelper;

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void upgradeCrtdl_schemaErrors_returns400() throws Exception {
    var issue = ValidationIssue.builder()
        .path("/foo")
        .value(ValidationIssueValue.builder()
            .code("VALIDATION-" + ValidationIssueType.JSON_ERROR.code())
            .message("something went wrong")
            .build())
        .build();
    doReturn(List.of(issue)).when(validationService).validateCrtdlSchema(any());

    mockMvc.perform(post(URI.create(PATH_API + PATH_UPGRADE + PATH_CRTDL)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(Map.of())))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void upgradeCrtdl_valid_returns200WithUpgradedCrtdl() throws Exception {
    var crtdl = Crtdl.builder().version("1.0.0").display("Test").build();
    var upgradedCrtdl = Crtdl.builder().version("1.0.0").display("Test upgraded").build();
    doReturn(List.of()).when(validationService).validateCrtdlSchema(any());
    doReturn(crtdl).when(validationService).crtdlFromJsonNode(any());
    doReturn(UpgradeWrapper.builder().crtdl(upgradedCrtdl).annotations(List.of()).build())
        .when(upgradeService).upgrade(any());

    mockMvc.perform(post(URI.create(PATH_API + PATH_UPGRADE + PATH_CRTDL)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(Map.of())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.crtdl.display").value("Test upgraded"))
        .andExpect(jsonPath("$.annotations").isEmpty());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void upgradeCrtdl_conditionOnsetDateFilter_returns200WithRecordedDateFilter() throws Exception {
    JsonNode crtdlNode;
    try (InputStream is = UpgradeRestControllerIT.class.getResourceAsStream("crtdl-condition-onset-date.json")) {
      crtdlNode = jsonUtil.readTree(is);
    }
    var dseService = mock(DseService.class);
    var dseProfile = DseProfile.builder()
        .url(DIAGNOSE_PROFILE_URL)
        .fields(List.of(
            Field.builder().id("Condition.code").build(),
            Field.builder().id("Condition.onset[x]").build(),
            Field.builder().id("Condition.recordedDate").build()))
        .filters(List.of(
            Filter.builder().type("date").name("recorded-date").uiType("timeRestriction").build(),
            Filter.builder().type("token").name("code").uiType("code").build()))
        .references(List.of())
        .build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(any());
    var realUpgradeService = new UpgradeService(dseService);
    doReturn(List.of()).when(validationService).validateCrtdlSchema(any());
    doAnswer(invocation -> jsonUtil.convertValue(invocation.<JsonNode>getArgument(0), Crtdl.class))
        .when(validationService).crtdlFromJsonNode(any());
    doAnswer(invocation -> realUpgradeService.upgrade(invocation.getArgument(0)))
        .when(upgradeService).upgrade(any());

    mockMvc.perform(post(URI.create(PATH_API + PATH_UPGRADE + PATH_CRTDL)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(crtdlNode)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.crtdl.dataExtraction.attributeGroups[0].filter.length()").value(2))
        .andExpect(jsonPath("$.crtdl.dataExtraction.attributeGroups[0].filter[0].type").value("date"))
        .andExpect(jsonPath("$.crtdl.dataExtraction.attributeGroups[0].filter[0].name").value("recorded-date"))
        .andExpect(jsonPath("$.crtdl.dataExtraction.attributeGroups[0].filter[0].start").value("2020-01-01"))
        .andExpect(jsonPath("$.crtdl.dataExtraction.attributeGroups[0].filter[0].end").value("2021-12-31"))
        .andExpect(jsonPath("$.crtdl.dataExtraction.attributeGroups[0].filter[1].name").value("code"))
        .andExpect(jsonPath("$.crtdl.dataExtraction.attributeGroups[0].filter[1].codes[0].code").value("E10-E14"))
        .andExpect(jsonPath("$.crtdl.dataExtraction.attributeGroups[0].attributes.length()").value(3))
        .andExpect(jsonPath("$.annotations.length()").value(1))
        .andExpect(jsonPath("$.annotations[0].path").value("dataExtraction/attributeGroups/0/filter/0"))
        .andExpect(jsonPath("$.annotations[0].value.code").value("UPGRADE-" + UpgradeIssueType.FILTER_CHANGE.code()))
        .andExpect(jsonPath("$.annotations[0].details.replaced.name").value("onset-date"))
        .andExpect(jsonPath("$.annotations[0].details.replacedWith.name").value("recorded-date"));
  }
}
