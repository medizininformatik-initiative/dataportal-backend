package de.numcodex.feasibility_gui_backend.query.api.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.dse.DseService;
import de.numcodex.feasibility_gui_backend.dse.api.DseProfile;
import de.numcodex.feasibility_gui_backend.query.api.Attribute;
import de.numcodex.feasibility_gui_backend.query.api.AttributeGroup;
import de.numcodex.feasibility_gui_backend.query.api.DataExtraction;
import de.numcodex.feasibility_gui_backend.query.api.Filter;
import de.numcodex.feasibility_gui_backend.query.api.status.ValidationIssue;
import de.numcodex.feasibility_gui_backend.terminology.es.CodeableConceptService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * Validator for {@link DataExtraction} that does an actual check based on a JSON schema.
 */
@Slf4j
public class DataExtractionValidator implements ConstraintValidator<DataExtractionValidation, DataExtraction> {

  private final CodeableConceptService codeableConceptService;

  private final DseService dseService;


  @NonNull
  private final ObjectMapper jsonUtil;

  /**
   * Required args constructor.
   * <p>
   * Lombok annotation had to be removed since it could not take the necessary Schema Qualifier
   */
  public DataExtractionValidator(CodeableConceptService codeableConceptService,
                                 DseService dseService,
                                 @NonNull ObjectMapper jsonUtil) {
    this.codeableConceptService = codeableConceptService;
    this.dseService = dseService;
    this.jsonUtil = jsonUtil;
  }

  /**
   * Validate the submitted {@link DataExtraction} against the json query schema.
   *
   * @param dataExtraction the {@link DataExtraction} to validate
   */
  @Override
  public boolean isValid(DataExtraction dataExtraction,
                         ConstraintValidatorContext ctx) {
    ctx.disableDefaultConstraintViolation();
    return !containsInvalidEntries(ctx, dataExtraction);
  }

  private boolean containsInvalidEntries(ConstraintValidatorContext ctx, DataExtraction dataExtraction) {
    if (dataExtraction == null || dataExtraction.attributeGroups() == null || dataExtraction.attributeGroups().isEmpty()) return false;
    var hasErrors = false;

    // Get all dse entries and save them in a map
    var referencedGroups = dataExtraction.attributeGroups().stream().map(AttributeGroup::groupReference).distinct().toList();
    var profileData = dseService.getProfileData(referencedGroups.stream().map(URI::toString).toList());

    for (int i = 0;  i < dataExtraction.attributeGroups().size(); i++) {
      // each groupReference can be resolved in ontology dse_profiles
      var attributeGroup = dataExtraction.attributeGroups().get(i);
      var dseProfileOptional = profileData.stream()
          .filter(p -> attributeGroup.groupReference().toString().equals(p.url()))
          .findFirst();
      DseProfile dseProfile = null;
      if (dseProfileOptional.isEmpty() || dseProfileOptional.get().errorCode() != null) {
        ValidationErrorBuilder.addError(
            ctx,
            MessageFormat.format("/attributeGroups/{0}/groupReference", i),
            ValidationIssue.ATTRIBUTE_GROUP_PROFILE_NOT_FOUND
        );
        hasErrors = true;
      } else {
        dseProfile = dseProfileOptional.get();
      }
      if (attributeGroup.attributes() != null) {
        hasErrors = attributesContainErrors(ctx, dataExtraction, attributeGroup, dseProfile,
            MessageFormat.format("/attributeGroups/{0}/attributes", i)) || hasErrors;
      }
      if (attributeGroup.filter() != null) {
        hasErrors = hasErrorsInAttributeGroupFilters(ctx, attributeGroup, dseProfile,
            MessageFormat.format("/attributeGroups/{0}", i)) || hasErrors;
      }
    }
    return hasErrors;
  }

  private boolean attributesContainErrors(ConstraintValidatorContext ctx,
                                          DataExtraction dataExtraction,
                                          AttributeGroup attributeGroup,
                                          DseProfile dseProfile,
                                          String jsonPointerBase) {
    var hasErrors = false;
    for (int i = 0; i < attributeGroup.attributes().size(); i++) {
      var attribute = attributeGroup.attributes().get(i);

      if (dseProfile != null) {
        // ensure linkedGroup exists for each attribute of type "reference" in dse profile
        if (dseProfile.references().stream().anyMatch(ref -> ref.id().equalsIgnoreCase(attribute.attributeRef()))) {
          if (attribute.linkedGroups() == null || attribute.linkedGroups().isEmpty()) {
            ValidationErrorBuilder.addError(
                ctx,
                MessageFormat.format("{0}/{1}/linkedGroups", jsonPointerBase, i),
                ValidationIssue.LINKED_GROUP_MISSING
            );
            hasErrors = true;
          }
        } else if (dseProfile.fields().stream().noneMatch(field -> field.id().equalsIgnoreCase(attribute.attributeRef()))) {
          // attributes of DSE features (attributeGroups) (fields) not in ontology profile for feature
          ValidationErrorBuilder.addError(
              ctx,
              MessageFormat.format("{0}/{1}/attributeRef", jsonPointerBase, i),
              ValidationIssue.ATTRIBUTE_REF_NOT_FOUND
          );
          hasErrors = true;
        }
      }
      if (attribute.linkedGroups() != null) {
        hasErrors = linkedGroupsContainInvalidLinks(ctx, dataExtraction, attribute,
            MessageFormat.format("{0}/{1}", jsonPointerBase, i)) || hasErrors;
      }
    }
    return hasErrors;
  }

  private boolean linkedGroupsContainInvalidLinks(ConstraintValidatorContext ctx,
                                                         DataExtraction dataExtraction,
                                                         Attribute attribute,
                                                         String jsonPointerBase) {
    var hasErrors = false;
    for (int i = 0; i < attribute.linkedGroups().size(); i++) {
      var linkedGroup = attribute.linkedGroups().get(i);
      if (dataExtraction.attributeGroups().stream().noneMatch(ag -> ag.id().equals(linkedGroup))) {
        ValidationErrorBuilder.addError(
            ctx,
            MessageFormat.format("{0}/linkedGroups/{1}", jsonPointerBase, i),
            ValidationIssue.LINKED_GROUP_NOT_FOUND
        );
        hasErrors = true;
      }
    }
    return hasErrors;
  }

  private boolean hasErrorsInAttributeGroupFilters(ConstraintValidatorContext ctx,
                                                   AttributeGroup attributeGroup,
                                                   DseProfile dseProfile,
                                                   String jsonPointerBase) {
    var hasErrors = false;
    for (int i = 0; i < attributeGroup.filter().size(); i++) {
      var attributeFilter = attributeGroup.filter().get(i);
        hasErrors = hasUnsupportedFilterTypes(ctx, dseProfile, attributeFilter,
            MessageFormat.format("{0}/filter/{1}", jsonPointerBase, i)) || hasErrors;
        hasErrors = codesMissingInValueSets(ctx, dseProfile, attributeFilter,
            MessageFormat.format("{0}/filter/{1}", jsonPointerBase, i)) || hasErrors;
      }
    return hasErrors;
  }

  private boolean hasUnsupportedFilterTypes(ConstraintValidatorContext ctx,
                                                   DseProfile dseProfile,
                                                   Filter attributeFilter,
                                                   String jsonPointerBase) {
    if (dseProfile.filters().stream().noneMatch(filter -> filter.type().equals(attributeFilter.type()))) {
      ValidationErrorBuilder.addError(
          ctx,
          MessageFormat.format("{0}/type", jsonPointerBase),
          ValidationIssue.FILTER_TYPE_NOT_SUPPORTED
      );
      return true;
    }
    return false;
  }

  private boolean codesMissingInValueSets(ConstraintValidatorContext ctx,
                                          DseProfile dseProfile,
                                          Filter attributeFilter,
                                          String jsonPointerBase) {
    var codes = attributeFilter.codes().stream().map(TermCode::code).toList();
    var valueSetUrls = dseProfile.filters().stream()
        .filter(filter -> filter.type().equals("token"))
        .flatMap(filter -> filter.valueSetUrls().stream())
        .distinct()
        .toList();
    if (!codes.isEmpty()) {
      var availableCodes = codeableConceptService.availableCodesInValueSets(codes, valueSetUrls);
      var unavailableCodes = new ArrayList<>(codes);
      unavailableCodes.removeAll(availableCodes);
      if (!unavailableCodes.isEmpty()) {
        for (var code : unavailableCodes) {
          ValidationErrorBuilder.addError(
              ctx,
              MessageFormat.format("{0}/codes/{1}", jsonPointerBase, codes.indexOf(code)),
              ValidationIssue.FILTER_CODE_NOT_FOUND
          );
        }
        return true;
      }
    }
    return false;
  }
}
