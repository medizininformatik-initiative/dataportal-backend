package de.medizininformatikinitiative.dataportal.backend.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.medizininformatikinitiative.dataportal.backend.dse.DseService;
import de.medizininformatikinitiative.dataportal.backend.dse.api.DseProfile;
import de.medizininformatikinitiative.dataportal.backend.dse.api.Field;
import de.medizininformatikinitiative.dataportal.backend.query.api.*;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.UpgradeIssue;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.UpgradeIssueType;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.UpgradeIssueValue;
import de.medizininformatikinitiative.dataportal.backend.validation.api.UpgradeWrapper;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.text.MessageFormat;
import java.util.*;

@Service
public class UpgradeService {

  private final DseService dseService;

  private final ObjectMapper jsonUtil;

  public UpgradeService(@NonNull DseService dseService,
                        @NonNull ObjectMapper jsonUtil) {
    this.dseService = dseService;
    this.jsonUtil = jsonUtil;
  }

  public UpgradeWrapper upgrade(@NonNull Crtdl crtdl) {
    if (crtdl.dataExtraction() == null || crtdl.dataExtraction().attributeGroups() == null || crtdl.dataExtraction().attributeGroups().isEmpty()) {
      return UpgradeWrapper.builder()
          .crtdl(crtdl)
          .annotations(List.of())
          .build();
    }
    List<UpgradeIssue> upgradeIssues = new ArrayList<>();
    DataExtraction fixedDataExtraction = crtdl.dataExtraction();

    // Get all dse entries and save them in a map
    var referencedGroups = crtdl.dataExtraction().attributeGroups().stream().map(AttributeGroup::groupReference).distinct().toList();
    var profileData = dseService.getProfileData(referencedGroups.stream().map(URI::toString).toList());

    for (int i = 0; i < crtdl.dataExtraction().attributeGroups().size(); ++i) {
      var attributeGroup = crtdl.dataExtraction().attributeGroups().get(i);
      var dseProfileOptional = profileData.stream()
          .filter(p -> attributeGroup.groupReference().toString().equals(p.url()))
          .findFirst();
      if (dseProfileOptional.isEmpty()) {
        throw new CrtdlUpgradeException("Dse Profile not found");
      }
      var dseProfile = dseProfileOptional.get();

      // UPGRADE-1000001: Filter Change
      var filterUpgradeResult = handleFilterNameChange(fixedDataExtraction, attributeGroup, dseProfile, i);
      if (!filterUpgradeResult.upgradeIssues().isEmpty()) {
        upgradeIssues.addAll(filterUpgradeResult.upgradeIssues());
      }
      fixedDataExtraction = filterUpgradeResult.dataExtraction();

      // UPGRADE-1000002: Field removed as no longer available + UPGRADE-1000003: Field changed to parent
      var fieldRemovedResult = handleFieldRemoved(fixedDataExtraction, attributeGroup, dseProfile, i);
      if (!fieldRemovedResult.upgradeIssues().isEmpty()) {
        upgradeIssues.addAll(fieldRemovedResult.upgradeIssues());
      }
      fixedDataExtraction = fieldRemovedResult.dataExtraction();

      // UPGRADE-1000004: Profile removed as it does not exist anymore
//      var profileRemovedResult = handleProfileRemoved(fixedDataExtraction, attributeGroup, dseProfile, i);
//      if (profileRemovedResult.upgradeIssue() != null) {
//        upgradeIssues.add(profileRemovedResult.upgradeIssue());
//      }
//      fixedDataExtraction = profileRemovedResult.dataExtraction();
    }


    return UpgradeWrapper.builder()
        .crtdl(Crtdl.builder()
            .version(crtdl.version())
            .display(crtdl.display())
            .cohortDefinition(crtdl.cohortDefinition())
            .dataExtraction(fixedDataExtraction)
            .build())
        .annotations(upgradeIssues)
        .build();
  }

  private DataExtractionUpgradeResult handleFilterNameChange(DataExtraction dataExtraction, AttributeGroup attributeGroup, DseProfile dseProfile, int groupIndex) {
    // Right now there should be only one date filter, and that is the only thing we can automatically fix here
    Optional<Filter> dateFilterCrtdl = attributeGroup.filter().stream().filter(f -> "date".equals(f.type())).findFirst();
    Optional<de.medizininformatikinitiative.dataportal.backend.dse.api.Filter> dateFilterProfile = dseProfile.filters().stream().filter(f -> "date".equals(f.type())).findFirst();

    if (dateFilterCrtdl.isPresent() && dateFilterProfile.isPresent()) {
      var filterCrtdl = dateFilterCrtdl.get();
      var filterProfile = dateFilterProfile.get();

      if (!filterCrtdl.name().equals(filterProfile.name())) {
        Filter fixedFilter = filterCrtdl.toBuilder()
            .name(filterProfile.name())
            .build();
        var fixedDataExtraction = DataExtraction.withReplacedDateFilter(dataExtraction,
            attributeGroup,
            fixedFilter);
        UpgradeIssue upgradeIssue = UpgradeIssue.builder()
            .path(MessageFormat.format("dataExtraction/attributeGroups/{0}/filter/{1}", groupIndex, attributeGroup.filter().indexOf(filterCrtdl)))
            .value(UpgradeIssueValue.builder()
                .message(UpgradeIssueType.FILTER_CHANGE.detail())
                .code("UPGRADE-" + UpgradeIssueType.FILTER_CHANGE.code())
                .build())
            .details(Map.of(
                "replaced", filterCrtdl,
                "replacedWith", fixedFilter
            ))
            .build();
        return DataExtractionUpgradeResult.builder()
            .dataExtraction(fixedDataExtraction)
            .upgradeIssues(List.of(upgradeIssue))
            .build();
      }
    }
    return DataExtractionUpgradeResult.builder()
        .dataExtraction(dataExtraction)
        .upgradeIssues(List.of())
        .build();
  }

  private DataExtractionUpgradeResult handleFieldRemoved(DataExtraction dataExtraction, AttributeGroup attributeGroup, DseProfile dseProfile, int groupIndex) {
    var fixedDataExtraction = dataExtraction;
    var fixedAttributeGroup = attributeGroup;
    var issues = new ArrayList<UpgradeIssue>();

    for (int attributeIndex = 0; attributeIndex < attributeGroup.attributes().size(); ++attributeIndex) {
      Attribute a = attributeGroup.attributes().get(attributeIndex);
      if (dseProfile.fields().stream().noneMatch(field -> fieldOrChildrenMatch(field, a.attributeRef()))) {
        var firstMatch = findFirstMatchingParent(a.attributeRef(), dseProfile);
        // In case the parent is already in the attribute group, remove it in the else-part
        if (firstMatch.isPresent()) {
          if (fixedAttributeGroup.attributes().stream().noneMatch(attr -> attr.attributeRef().equals(firstMatch.get()))) {
            var fixedAttribute = a.toBuilder()
                .attributeRef(firstMatch.get())
                .build();
            fixedDataExtraction = DataExtraction.withReplacedAttribute(fixedDataExtraction, fixedAttributeGroup, a, fixedAttribute);
            fixedAttributeGroup = findAttributeGroup(fixedDataExtraction, attributeGroup.id());
            UpgradeIssue upgradeIssue = UpgradeIssue.builder()
                .path(MessageFormat.format("dataExtraction/attributeGroups/{0}/attributes/{1}", groupIndex, attributeIndex))
                .value(UpgradeIssueValue.builder()
                    .message(UpgradeIssueType.FIELD_CHANGED_TO_PARENT.detail())
                    .code("UPGRADE-" + UpgradeIssueType.FIELD_CHANGED_TO_PARENT.code())
                    .build())
                .details(Map.of(
                    "replaced", a,
                    "replacedWith", fixedAttribute
                ))
                .build();
            issues.add(upgradeIssue);
          } else {
            // TODO: this just duplicates the else tree below...it works but...make this smarter
            fixedDataExtraction = DataExtraction.withRemovedAttribute(fixedDataExtraction, fixedAttributeGroup, a);
            fixedAttributeGroup = findAttributeGroup(fixedDataExtraction, attributeGroup.id());
            UpgradeIssue upgradeIssue = UpgradeIssue.builder()
                .path(MessageFormat.format("dataExtraction/attributeGroups/{0}/attributes/{1}", groupIndex, attributeIndex))
                .value(UpgradeIssueValue.builder()
                    .message(UpgradeIssueType.FIELD_NO_LONGER_AVAILABLE.detail())
                    .code("UPGRADE-" + UpgradeIssueType.FIELD_NO_LONGER_AVAILABLE.code())
                    .build())
                .details(Map.of(
                    "replaced", a
                ))
                .build();
            issues.add(upgradeIssue);
          }
        } else {
          fixedDataExtraction = DataExtraction.withRemovedAttribute(fixedDataExtraction, attributeGroup, a);
          fixedAttributeGroup = findAttributeGroup(fixedDataExtraction, attributeGroup.id());
          UpgradeIssue upgradeIssue = UpgradeIssue.builder()
              .path(MessageFormat.format("dataExtraction/attributeGroups/{0}/attributes/{1}", groupIndex, attributeIndex))
              .value(UpgradeIssueValue.builder()
                  .message(UpgradeIssueType.FIELD_NO_LONGER_AVAILABLE.detail())
                  .code("UPGRADE-" + UpgradeIssueType.FIELD_NO_LONGER_AVAILABLE.code())
                  .build())
              .details(Map.of(
                  "replaced", a
              ))
              .build();
          issues.add(upgradeIssue);
        }
      }
    }
    return DataExtractionUpgradeResult.builder()
        .dataExtraction(fixedDataExtraction)
        .upgradeIssues(issues)
        .build();
  }

  public static Optional<String> findFirstMatchingParent(String attributeRef, DseProfile dseProfile) {
    String current = attributeRef;

    while (true) {
      String finalCurrent = current;
      if (dseProfile.fields().stream().anyMatch(field -> field.id().equals(finalCurrent))) {
        return Optional.of(current);
      }

      int lastDot = current.lastIndexOf('.');
      if (lastDot < 0) {
        break;
      }

      current = current.substring(0, lastDot);
    }

    return Optional.empty();
  }

  private boolean fieldOrChildrenMatch(Field field, String attributeRef) {

    if (field.id().equalsIgnoreCase(attributeRef)) {
      return true;
    }

    if (field.children() != null && !field.children().isEmpty()) {
      return field.children().stream()
          .anyMatch(child -> fieldOrChildrenMatch(child, attributeRef));
    }

    return false;
  }

  private AttributeGroup findAttributeGroup(DataExtraction extraction, String id) {
    return extraction.attributeGroups().stream()
        .filter(g -> Objects.equals(g.id(), id))
        .findFirst()
        .orElseThrow();
  }
}
