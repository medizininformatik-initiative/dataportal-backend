package de.medizininformatikinitiative.dataportal.backend.query.api.validation;

import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssueType;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("query")
@Tag("api")
@Tag("validation")
@ExtendWith(MockitoExtension.class)
class ValidationErrorBuilderTest {

  private static final JsonMapper JSON_UTIL = JsonMapper.builderWithJackson2Defaults().build();

  @Mock
  private ConstraintValidatorContext ctx;

  @Mock
  private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

  @BeforeEach
  void setUp() {
    when(ctx.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    when(violationBuilder.addConstraintViolation()).thenReturn(ctx);
  }

  @Test
  void addError_withIssueType_buildsTemplateFromTypeAndAddsViolation() {
    ValidationErrorBuilder.addError(ctx, "/attributeGroups/0", ValidationIssueType.ATTRIBUTE_REF_NOT_FOUND);

    var error = captureError();
    assertThat(error.get("path")).isEqualTo("/attributeGroups/0");
    assertThat(value(error).get("code")).isEqualTo("VALIDATION-" + ValidationIssueType.ATTRIBUTE_REF_NOT_FOUND.code());
    assertThat(value(error).get("message")).isEqualTo(ValidationIssueType.ATTRIBUTE_REF_NOT_FOUND.detail());
    assertThat((Map<?, ?>) error.get("details")).isEmpty();
    verify(violationBuilder).addConstraintViolation();
  }

  @Test
  void addError_withExplicitCodeAndMessage_buildsTemplateVerbatim() {
    ValidationErrorBuilder.addError(ctx, "/foo", "CUSTOM-1", "Custom message");

    var error = captureError();
    assertThat(error.get("path")).isEqualTo("/foo");
    assertThat(value(error).get("code")).isEqualTo("CUSTOM-1");
    assertThat(value(error).get("message")).isEqualTo("Custom message");
    assertThat((Map<?, ?>) error.get("details")).isEmpty();
  }

  @Test
  void addError_withIssueTypeAndExtra_includesExtraDetails() {
    ValidationErrorBuilder.addError(ctx, "/foo/0", ValidationIssueType.FILTER_CODE_NOT_FOUND,
        Map.of("code", "123", "system", "http://example.org"));

    var error = captureError();
    assertThat(value(error).get("code")).isEqualTo("VALIDATION-" + ValidationIssueType.FILTER_CODE_NOT_FOUND.code());
    var details = (Map<?, ?>) error.get("details");
    assertThat(details.get("code")).isEqualTo("123");
    assertThat(details.get("system")).isEqualTo("http://example.org");
  }

  @Test
  @SuppressWarnings("unchecked")
  void addError_withExplicitCodeMessageAndExtra_includesAllFields() {
    ValidationErrorBuilder.addError(ctx, "/bar", "CUSTOM-2", "Custom message", Map.of("foo", "bar"));

    var error = captureError();
    assertThat(error.get("path")).isEqualTo("/bar");
    assertThat(value(error).get("code")).isEqualTo("CUSTOM-2");
    assertThat(value(error).get("message")).isEqualTo("Custom message");
    assertThat((Map<String, Object>) error.get("details")).containsEntry("foo", "bar");
  }

  @Test
  void addError_withNullExtra_detailsIsEmpty() {
    ValidationErrorBuilder.addError(ctx, "/bar", "CUSTOM-2", "Custom message", null);

    var error = captureError();
    assertThat((Map<?, ?>) error.get("details")).isEmpty();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> value(Map<String, Object> error) {
    return (Map<String, Object>) error.get("value");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> captureError() {
    var captor = ArgumentCaptor.forClass(String.class);
    verify(ctx).buildConstraintViolationWithTemplate(captor.capture());
    return JSON_UTIL.readValue(captor.getValue(), Map.class);
  }
}
