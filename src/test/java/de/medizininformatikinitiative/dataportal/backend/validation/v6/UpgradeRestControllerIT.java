package de.medizininformatikinitiative.dataportal.backend.validation.v6;

import de.medizininformatikinitiative.dataportal.backend.query.api.Crtdl;
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
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
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
}
