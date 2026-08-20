package de.medizininformatikinitiative.dataportal.backend.validation.v6;

import de.medizininformatikinitiative.dataportal.backend.query.api.Ccdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.Crtdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.Dataquery;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssue;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssueType;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssueValue;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.AuthenticationHelper;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.medizininformatikinitiative.dataportal.backend.validation.ValidationService;
import org.hl7.fhir.utilities.tests.TestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("query")
@Tag("validation")
@ExtendWith(SpringExtension.class)
@Import({RateLimitingServiceSpringConfig.class, TestConfig.class})
@WebMvcTest(controllers = ValidationRestController.class)
class ValidationRestControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper jsonUtil;

  @MockitoBean
  private ValidationService validationService;

  @MockitoBean
  private AuthenticationHelper authenticationHelper;

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void validateCcdl_schemaErrors_returns400() throws Exception {
    doReturn(List.of(schemaIssue())).when(validationService).validateCcdlSchema(any());

    mockMvc.perform(post(URI.create(PATH_API + PATH_VALIDATION + PATH_CCDL)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(Map.of())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$[0].value.code").value("VALIDATION-" + ValidationIssueType.JSON_ERROR.code()));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void validateCcdl_valid_returns200() throws Exception {
    doReturn(List.of()).when(validationService).validateCcdlSchema(any());
    doReturn(Ccdl.builder().build()).when(validationService).ccdlFromJsonNode(any());

    mockMvc.perform(post(URI.create(PATH_API + PATH_VALIDATION + PATH_CCDL)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(Map.of())))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void validateCcdl_contentInvalid_returns400ViaAdvice() throws Exception {
    doReturn(List.of()).when(validationService).validateCcdlSchema(any());
    doReturn(Ccdl.builder().build()).when(validationService).ccdlFromJsonNode(any());
    doThrow(buildMethodArgumentNotValidException("ccdl", "display")).when(validationService).validateCcdlContent(any());

    mockMvc.perform(post(URI.create(PATH_API + PATH_VALIDATION + PATH_CCDL)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(Map.of())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$[0].value.code").value("VALIDATION-1"))
        .andExpect(jsonPath("$[0].path").value("display/display"));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void validateCrtdl_schemaErrors_returns400() throws Exception {
    doReturn(List.of(schemaIssue())).when(validationService).validateCrtdlSchema(any());

    mockMvc.perform(post(URI.create(PATH_API + PATH_VALIDATION + PATH_CRTDL)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(Map.of())))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void validateCrtdl_valid_returns200() throws Exception {
    doReturn(List.of()).when(validationService).validateCrtdlSchema(any());
    doReturn(Crtdl.builder().build()).when(validationService).crtdlFromJsonNode(any());

    mockMvc.perform(post(URI.create(PATH_API + PATH_VALIDATION + PATH_CRTDL)).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(Map.of())))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void validateDataquery_schemaErrors_returns400() throws Exception {
    doReturn(List.of(schemaIssue())).when(validationService).validateDataquerySchema(any());

    mockMvc.perform(post(URI.create(PATH_API + PATH_VALIDATION + "/dataquery")).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(Map.of())))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void validateDataquery_valid_returns200() throws Exception {
    doReturn(List.of()).when(validationService).validateDataquerySchema(any());
    doReturn(Dataquery.builder().build()).when(validationService).dataqueryFromJsonNode(any());

    mockMvc.perform(post(URI.create(PATH_API + PATH_VALIDATION + "/dataquery")).with(csrf())
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(Map.of())))
        .andExpect(status().isOk());
  }

  private ValidationIssue schemaIssue() {
    return ValidationIssue.builder()
        .path("/foo")
        .value(ValidationIssueValue.builder()
            .code("VALIDATION-" + ValidationIssueType.JSON_ERROR.code())
            .message("something went wrong")
            .build())
        .build();
  }

  private MethodArgumentNotValidException buildMethodArgumentNotValidException(String objectName, String field) throws NoSuchMethodException {
    var bindingResult = new BeanPropertyBindingResult(new Object(), objectName);
    bindingResult.addError(new FieldError(objectName, field,
        "{\"path\":\"/" + field + "\",\"value\":{\"code\":\"VALIDATION-1\",\"message\":\"Invalid value\"}}"));
    var methodParameter = new MethodParameter(Object.class.getMethod("equals", Object.class), 0);
    return new MethodArgumentNotValidException(methodParameter, bindingResult);
  }
}
