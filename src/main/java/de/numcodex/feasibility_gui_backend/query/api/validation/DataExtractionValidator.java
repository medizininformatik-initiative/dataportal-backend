package de.numcodex.feasibility_gui_backend.query.api.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.erosb.jsonsKema.*;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.dse.DseService;
import de.numcodex.feasibility_gui_backend.query.api.AttributeGroup;
import de.numcodex.feasibility_gui_backend.query.api.DataExtraction;
import de.numcodex.feasibility_gui_backend.query.api.status.ValidationIssue;
import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import de.numcodex.feasibility_gui_backend.terminology.es.CodeableConceptService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * Validator for {@link DataExtraction} that does an actual check based on a JSON schema.
 */
@Slf4j
public class DataExtractionValidator implements ConstraintValidator<DataExtractionValidation, DataExtraction> {

  private final TerminologyService terminologyService;

  private final CodeableConceptService codeableConceptService;

  private final DseService dseService;

  private final Validator validator;


  @NonNull
  private final ObjectMapper jsonUtil;

  /**
   * Required args constructor.
   * <p>
   * Lombok annotation had to be removed since it could not take the necessary Schema Qualifier
   */
  public DataExtractionValidator(@Qualifier(value = "dataExtraction") Schema jsonSchema,
                                 TerminologyService terminologyService,
                                 CodeableConceptService codeableConceptService,
                                 DseService dseService,
                                 ObjectMapper jsonUtil) {
    this.terminologyService = terminologyService;
    this.codeableConceptService = codeableConceptService;
    this.dseService = dseService;
    this.jsonUtil = jsonUtil;
    this.validator = Validator.create(jsonSchema, new ValidatorConfig(FormatValidationPolicy.ALWAYS));
  }

  /**
   * Validate the submitted {@link DataExtraction} against the json query schema.
   *
   * @param dataExtraction the {@link DataExtraction} to validate
   */
  @Override
  public boolean isValid(DataExtraction dataExtraction,
                         ConstraintValidatorContext ctx) {
    boolean valid = true;
    ctx.disableDefaultConstraintViolation();

    try {
      var jsonSubject = new JsonParser(jsonUtil.writeValueAsString(dataExtraction)).parse();
      var validationFailure = validator.validate(jsonSubject);
      if (validationFailure != null) {
        ValidationErrorBuilder.addError(
            ctx,
            "",
            "VALIDATION-0000000",
            "Check the error message stream for what to include and how"
        );
        log.error("DataExtraction is invalid: {}", validationFailure.getMessage());
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

    valid = valid && !containsInvalidEntries(ctx, dataExtraction);

    return valid;
  }

  // TODO: Split this method in smaller methods
  private boolean containsInvalidEntries(ConstraintValidatorContext ctx, DataExtraction dataExtraction) {
    if (dataExtraction == null || dataExtraction.attributeGroups() == null || dataExtraction.attributeGroups().isEmpty()) return false;
    var hasErrors = false;

    // Get all dse entries and save them in a map
    var referencedGroups = dataExtraction.attributeGroups().stream().map(AttributeGroup::groupReference).distinct().toList();
    var profileData = dseService.getProfileData(referencedGroups.stream().map(URI::toString).toList());

    for (int i = 0;  i < dataExtraction.attributeGroups().size(); i++) {
      // 1) each groupReference can be resolved in ontology dse_profiles
      var attributeGroup = dataExtraction.attributeGroups().get(i);
      var dseProfile = profileData.stream()
          .filter(p -> attributeGroup.groupReference().toString().equals(p.url()))
          .findFirst();
      if (dseProfile.isEmpty() || dseProfile.get().errorCode() != null) {
        ValidationErrorBuilder.addError(
            ctx,
            MessageFormat.format("/attributeGroups/{0}/groupReference", i),
            ValidationIssue.ATTRIBUTE_GROUP_PROFILE_NOT_FOUND
        );
        hasErrors = true;
      }
      if (attributeGroup.attributes() != null) {
        for (int j = 0; j < attributeGroup.attributes().size(); j++) {
          var attribute = attributeGroup.attributes().get(j);

          if (dseProfile.isPresent() && dseProfile.get().errorCode() == null) {
            // 2) ensure linkedGroup exists for each attribute of type "reference" in dse profile
            if (dseProfile.get().references().stream().anyMatch(ref -> ref.id().equalsIgnoreCase(attribute.attributeRef()))) {
              if (attribute.linkedGroups() == null || attribute.linkedGroups().isEmpty()) {
                ValidationErrorBuilder.addError(
                    ctx,
                    MessageFormat.format("/attributeGroups/{0}/attributes/{1}/linkedGroups", i, j),
                    ValidationIssue.LINKED_GROUP_MISSING
                );
                hasErrors = true;
              }
            } else if (dseProfile.get().fields().stream().noneMatch(field -> field.id().equalsIgnoreCase(attribute.attributeRef()))) {
              // 3) attributes of DSE features (attributeGroups) (fields) not in ontology profile for feature
              ValidationErrorBuilder.addError(
                  ctx,
                  MessageFormat.format("/attributeGroups/{0}/attributes/{1}/attributeRef", i, j),
                  ValidationIssue.ATTRIBUTE_REF_NOT_FOUND
              );
              hasErrors = true;
            }
          }

          // 3) if linkedGroup exists ensure that ids it links to exist in the CRTDL
          if (attribute.linkedGroups() != null) {
            for (int k = 0; k < attribute.linkedGroups().size(); k++) {
              var linkedGroup = attribute.linkedGroups().get(k);
              if (dataExtraction.attributeGroups().stream().noneMatch(ag -> ag.id().equals(linkedGroup))) {
                ValidationErrorBuilder.addError(
                    ctx,
                    MessageFormat.format("/attributeGroups/{0}/attributes/{1}/linkedGroups/{2}", i, j, k),
                    ValidationIssue.LINKED_GROUP_NOT_FOUND
                );
                hasErrors = true;
              }
            }
          }
        }
      }

      if (attributeGroup.filter() != null) {
        for (int j = 0; j < attributeGroup.filter().size(); j++) {
          var attributeFilter = attributeGroup.filter().get(j);
          // 4) filter not valid (e.g. codes not available in value set anymore) -> code available in val set, filter in general allowed (e.g. date, code) for attributeGroup
          if (dseProfile.isPresent() && dseProfile.get().errorCode() == null) {
            // Check if filter type is allowed
            if (dseProfile.get().filters().stream().noneMatch(filter -> filter.type().equals(attributeFilter.type()))) {
              ValidationErrorBuilder.addError(
                  ctx,
                  MessageFormat.format("/attributeGroups/{0}/filter/{1}/type", i, j),
                  ValidationIssue.FILTER_TYPE_NOT_SUPPORTED
              );
              hasErrors = true;
            }
            // Check if codes are available in valuesets
            var codes = attributeFilter.codes().stream().map(TermCode::code).toList();
            var valueSetUrls = dseProfile.get().filters().stream()
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
                      MessageFormat.format("/attributeGroups/{0}/filter/{1}/codes/{2}", i, j, codes.indexOf(code)),
                      ValidationIssue.FILTER_CODE_NOT_FOUND
                  );
                }
                hasErrors = true;
              }
            }
          }

        }
      }

    }

    return hasErrors;
  }




}
