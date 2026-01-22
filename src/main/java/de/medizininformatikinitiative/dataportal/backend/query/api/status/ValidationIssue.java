package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@JsonSerialize(using = ValidationIssueSerializer.class)
public enum ValidationIssue {
  JSON_ERROR(10000, "JSON Schema validation failed."),
  CONTEXT_MISSING(10001, "Context missing."),
  CCDL_FILTER_CODE_NOT_FOUND(1000001, "code filter not found in value set"),
  TERMCODE_CONTEXT_COMBINATION_INVALID(20001, "The combination of context and termcode(s) is not found."),
  TERMCODE_NOT_IN_SYSTEM(20002, "The termcode does not exist in the system."),
  TIMERESTRICTION_INVALID(20003, "The TimeRestriction is invalid. 'beforeDate' must not be before 'afterDate'"),
  VALUEFILTER_INVALID_UNIT(20004, "The selected unit is invalid."),
  VALUEFILTER_OUT_OF_BOUNDS(20005, "The selected value in not inside the defined limits."),
  CODE_NOT_IN_REFERENCED_CRITERIA_SET(20006, "The selected code is not part of the referenced criteria sets."),
  CODE_NOT_IN_REFERENCED_VALUE_SET(20007, "The selected code is not part of the referenced value sets."),
  VALUEFILTER_MIN_MAX_ERROR(20008, "The minimum must not be larger than the maximum."),
  ATTRIBUTE_GROUP_PROFILE_NOT_FOUND(2000001, "Attribute group groupReference not found"),
  ATTRIBUTE_REF_NOT_FOUND(2000002, "attributeRef not found in ontology profile of attribute group"),
  FILTER_CODE_NOT_FOUND(2000003, "code filter not found in value set"),
  FILTER_TYPE_NOT_SUPPORTED(2000004, "code filter not found in value set"),
  LINKED_GROUP_MISSING(2000005, "Linked group missing for attribute of type reference"),
  LINKED_GROUP_NOT_FOUND(2000006, "Linked group not found in CRTDL"),
  UIPROFILE_NOT_FOUND(50001, "UiProfile not found");

  private static final ValidationIssue[] VALUES;

  static {
    VALUES = values();
  }

  private final int code;
  private final String detail;

  ValidationIssue(int code, String detail) {
    this.code = code;
    this.detail = detail;
  }

  public static ValidationIssue valueOf(int validationIssueCode) {
    ValidationIssue validationIssue = resolve(validationIssueCode);
    if (validationIssue == null) {
      throw new IllegalArgumentException("No matching Validation issue for code " + validationIssueCode);
    }
    return validationIssue;
  }

  @Nullable
  public static ValidationIssue resolve(int validationIssueCode) {
    for (ValidationIssue validationIssue : VALUES) {
      if (validationIssue.code == validationIssueCode) {
        return validationIssue;
      }
    }
    return null;
  }

  public int code() {
    return this.code;
  }

  public String detail() {
    return this.detail;
  }
}
