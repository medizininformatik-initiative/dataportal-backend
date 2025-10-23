package de.numcodex.feasibility_gui_backend.query.api.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.erosb.jsonsKema.*;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.TimeRestriction;
import de.numcodex.feasibility_gui_backend.query.api.ValueFilterType;
import de.numcodex.feasibility_gui_backend.query.api.status.ValidationIssue;
import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import de.numcodex.feasibility_gui_backend.terminology.UiProfileNotFoundException;
import de.numcodex.feasibility_gui_backend.terminology.api.UiProfile;
import de.numcodex.feasibility_gui_backend.terminology.api.ValueDefinitonType;
import de.numcodex.feasibility_gui_backend.terminology.es.CodeableConceptService;
import de.numcodex.feasibility_gui_backend.terminology.es.TerminologyEsService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Qualifier;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Validator for {@link StructuredQuery} that does an actual check based on a JSON schema.
 */
@Slf4j
public class StructuredQueryValidator implements ConstraintValidator<StructuredQueryValidation, StructuredQuery> {

  private final static String IGNORED_CONSENT_SYSTEM = "fdpg.consent.combined";

  private final TerminologyService terminologyService;

  private final TerminologyEsService terminologyEsService;

  private final CodeableConceptService codeableConceptService;

  @NonNull
  private final ObjectMapper jsonUtil;

  private final Validator validator;

  /**
   * Required args constructor.
   * <p>
   * Lombok annotation had to be removed since it could not take the necessary Schema Qualifier
   */
  public StructuredQueryValidator(@Qualifier(value = "ccdl") Schema jsonSchema,
                                  TerminologyService terminologyService,
                                  TerminologyEsService terminologyEsService,
                                  CodeableConceptService codeableConceptService,
                                  ObjectMapper jsonUtil) {
    this.terminologyService = terminologyService;
    this.terminologyEsService = terminologyEsService;
    this.codeableConceptService = codeableConceptService;
    this.jsonUtil = jsonUtil;
    this.validator = Validator.create(jsonSchema, new ValidatorConfig(FormatValidationPolicy.ALWAYS));
  }

  /**
   * Validate the submitted {@link StructuredQuery} against the json query schema.
   *
   * @param structuredQuery the {@link StructuredQuery} to validate
   */
  @Override
  public boolean isValid(StructuredQuery structuredQuery,
                         ConstraintValidatorContext ctx) {
    boolean valid = true;
    ctx.disableDefaultConstraintViolation();

    try {
      var jsonSubject = new JsonParser(jsonUtil.writeValueAsString(structuredQuery)).parse();
      var validationFailure = validator.validate(jsonSubject);
      if (validationFailure != null) {
        ValidationErrorBuilder.addError(
            ctx,
            "",
            "VALIDATION-0000000",
            "Check the error message stream for what to include and how"
        );
        log.error("Structured query is invalid: {}", validationFailure.getMessage());
        valid = false;
      }
    } catch (JsonProcessingException jpe) {
      ValidationErrorBuilder.addError(
          ctx,
          "/",
          ValidationIssue.JSON_ERROR
      );
      log.debug("Could not process JSON", jpe);
      valid = false;
    }

    valid = valid && !containsInvalidCriteria(ctx, structuredQuery);
    return valid ;
  }

  private boolean containsInvalidCriteria(ConstraintValidatorContext ctx, StructuredQuery structuredQuery) {
    var hasErrors = containsInvalidCriteria(ctx, "/inclusionCriteria", structuredQuery.inclusionCriteria());
    return containsInvalidCriteria(ctx, "/exclusionCriteria", structuredQuery.exclusionCriteria())
        || hasErrors;
  }

  private boolean containsInvalidCriteria(ConstraintValidatorContext ctx, String jsonPointerBase, List<List<Criterion>> criteria) {
    if (criteria == null || criteria.isEmpty()) return false;
    var hasErrors = false;

    for (int i = 0; i < criteria.size(); i++) {
      var innerCriteria = criteria.get(i);
      for (int j = 0; j < innerCriteria.size(); j++) {
        Criterion criterion = innerCriteria.get(j);

        if (criterion.context() == null) {
          ValidationErrorBuilder.addError(
              ctx,
              MessageFormat.format("{0}/{1}/{2}", jsonPointerBase, i, j),
              ValidationIssue.CONTEXT_MISSING
          );
          hasErrors = true;
          continue;
        }

        if (isTimeRestrictionInvalid(criterion.timeRestriction())) {
          ValidationErrorBuilder.addError(
              ctx,
              MessageFormat.format("{0}/{1}/{2}/timeRestriction", jsonPointerBase, i, j),
              ValidationIssue.TIMERESTRICTION_INVALID
          );
          hasErrors = true;
        }

        hasErrors = checkForInvalidSelectedConceptsAndFilters(ctx, MessageFormat.format("{0}/{1}/{2}", jsonPointerBase, i, j), criterion) || hasErrors;

        List<TermCode> termCodes = criterion.termCodes();
        for (int k = 0; k < termCodes.size(); k++) {
          TermCode termCode = termCodes.get(k);
          if (termCode.system().equalsIgnoreCase(IGNORED_CONSENT_SYSTEM)) {
            continue;
          }
          if (!terminologyService.isExistingTermCode(termCode.system(), termCode.code())) {
            ValidationErrorBuilder.addError(
                ctx,
                MessageFormat.format("{0}/{1}/{2}/termCodes/{3}", jsonPointerBase, i, j, k),
                ValidationIssue.TERMCODE_NOT_IN_SYSTEM,
                Map.of("termcode", termCode)
            );
            hasErrors = true;
          }
        }
      }
    }
    return hasErrors;
  }

  private boolean checkForInvalidSelectedConceptsAndFilters(ConstraintValidatorContext ctx, String jsonPointerBase, Criterion criterion) {
    var hasErrors = false;
    // 1 Get UI Profile
    try {
      var uiProfileString = terminologyService.getUiProfile(TerminologyEsService.createContextualizedTermcodeHash(criterion));
      var uiProfile = jsonUtil.readValue(uiProfileString, UiProfile.class);
      // Check the valueFilter
      if (criterion.valueFilter() != null && criterion.valueFilter().type() == ValueFilterType.CONCEPT) {
        var codes = criterion.valueFilter().selectedConcepts().stream()
            .map(TermCode::code)
            .distinct()
            .toList();
        // 2 Extract value sets
        var valueSetUrls = uiProfile.valueDefinition().referencedValueSets().stream()
            .distinct()
            .toList();
        // 3 check with elastic search - which selected concepts can be found
        if (!codes.isEmpty()) {
          var availableCodes = codeableConceptService.availableCodesInValueSets(codes, valueSetUrls);
          var unavailableCodes = new ArrayList<>(codes);
          unavailableCodes.removeAll(availableCodes);
          if (!unavailableCodes.isEmpty()) {
            for (var code : unavailableCodes) {
              ValidationErrorBuilder.addError(
                  ctx,
                  MessageFormat.format("{0}/valueFilter/selectedConcepts/{1}", jsonPointerBase, codes.indexOf(code)),
                  ValidationIssue.CCDL_FILTER_CODE_NOT_FOUND
              );
            }
            hasErrors = true;
          }
        }
      }

      // Check the attributeFilters
      // TODO: this will probably need a rework to correctly get the indices...
      if (criterion.attributeFilters() != null && !criterion.attributeFilters().isEmpty()) {
        var referenceCodes = criterion.attributeFilters().stream()
            .filter(filter -> filter.type() == ValueFilterType.REFERENCE)
            .flatMap(filter -> filter.criteria().stream())
            .flatMap(c -> c.termCodes().stream())
            .map(TermCode::code)
            .distinct()
            .toList();
        var selectedConceptCodes = criterion.attributeFilters().stream()
            .filter(filter -> filter.type() == ValueFilterType.CONCEPT)
            .flatMap(filter -> filter.selectedConcepts().stream())
            .map(TermCode::code)
            .distinct()
            .toList();
        // 2 Extract value sets
        var referencedCriteriaSetUrlsTypeReference = uiProfile.attributeDefinitions().stream()
            .filter(Objects::nonNull)
            .filter(f -> f.type() == ValueDefinitonType.REFERENCE)
            .flatMap(ad -> ad.referencedCriteriaSets().stream())
            .distinct()
            .toList();
        var referencedValueSetUrls = uiProfile.attributeDefinitions().stream()
            .filter(Objects::nonNull)
            .filter(f -> f.type() == ValueDefinitonType.CONCEPT)
            .flatMap(ad -> ad.referencedValueSets().stream())
            .distinct()
            .toList();


        if (!referenceCodes.isEmpty()) {
          var availableCodes = terminologyEsService.availableCodesInReferencedCriteriaSets(referenceCodes, referencedCriteriaSetUrlsTypeReference);
          var unavailableCodes = new ArrayList<>(referenceCodes);
          unavailableCodes.removeAll(availableCodes);
          if (!unavailableCodes.isEmpty()) {
            for (var code : unavailableCodes) {
              ValidationErrorBuilder.addError(
                  ctx,
                  MessageFormat.format("{0}/attributeFilters/{1}", jsonPointerBase, referenceCodes.indexOf(code)), // TODO: this will probably not be correct...
                  ValidationIssue.CODE_NOT_IN_REFERENCED_CRITERIA_SET,
                  Map.of("code", code,
                      "criteriaSets", referencedCriteriaSetUrlsTypeReference)
              );
            }
            hasErrors = true;
          }
        }

        if (!selectedConceptCodes.isEmpty()) {
          var availableCodes = codeableConceptService.availableCodesInValueSets(selectedConceptCodes, referencedValueSetUrls);
          var unavailableCodes = new ArrayList<>(selectedConceptCodes);
          unavailableCodes.removeAll(availableCodes);
          if (!unavailableCodes.isEmpty()) {
            for (var code : unavailableCodes) {
              ValidationErrorBuilder.addError(
                  ctx,
                  MessageFormat.format("{0}/attributeFilters/{1}", jsonPointerBase, selectedConceptCodes.indexOf(code)), // TODO: this will probably not be correct...
                  ValidationIssue.CODE_NOT_IN_REFERENCED_VALUE_SET,
                  Map.of("code", code,
                      "valueSets", referencedValueSetUrls)
              );
            }
            hasErrors = true;
          }
        }

      }

      // 4 check if units are allowed
      var valueDefinition = uiProfile.valueDefinition();
      if (valueDefinition != null) {
        if (valueDefinition.allowedUnits() != null && !valueDefinition.allowedUnits().isEmpty()) {
          if (valueDefinition.allowedUnits().stream().noneMatch(unit -> unit.code().equals(criterion.valueFilter().quantityUnit().code()))) {
            ValidationErrorBuilder.addError(
                ctx,
                MessageFormat.format("{0}/valueFilter/unit", jsonPointerBase),
                ValidationIssue.VALUEFILTER_INVALID_UNIT,
                Map.of("selected", criterion.valueFilter().quantityUnit().code(),
                    "allowed", valueDefinition.allowedUnits().stream()
                        .map(TermCode::code)
                        .filter(Objects::nonNull)
                        .map(code -> "'" + code + "'")
                        .collect(Collectors.joining(",", "[", "]")))
            );
            hasErrors = true;
          }
        }

        // 5 check if quantity values are indeed numbers
        if (valueDefinition.type() == ValueDefinitonType.QUANTITY) {
          if (valueDefinition.max() != null && criterion.valueFilter().value() > valueDefinition.max()) {
            ValidationErrorBuilder.addError(
                ctx,
                MessageFormat.format("{0}/valueFilter/value", jsonPointerBase),
                ValidationIssue.VALUEFILTER_OUT_OF_BOUNDS
            );
            hasErrors = true;
          }
          if (valueDefinition.min() != null && criterion.valueFilter().value() < valueDefinition.min()) {
            ValidationErrorBuilder.addError(
                ctx,
                MessageFormat.format("{0}/valueFilter/value", jsonPointerBase),
                ValidationIssue.VALUEFILTER_OUT_OF_BOUNDS
            );
            hasErrors = true;
          }
        }
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    } catch (UiProfileNotFoundException uie) {
      ValidationErrorBuilder.addError(
          ctx,
          jsonPointerBase,
          ValidationIssue.UIPROFILE_NOT_FOUND
      );
      hasErrors = true;
    }
    return hasErrors;
  }


  private boolean isTimeRestrictionInvalid(TimeRestriction timeRestriction) {
    // If no timeRestriction is set or only on of both dates is set, it is not invalid
    if (timeRestriction == null || timeRestriction.beforeDate() == null || timeRestriction.afterDate() == null) {
      return false;
    }
    return LocalDate.parse(timeRestriction.beforeDate()).isBefore(LocalDate.parse(timeRestriction.afterDate()));
  }
}
