package de.medizininformatikinitiative.dataportal.backend.validation;

import de.medizininformatikinitiative.dataportal.backend.dse.DseService;
import de.medizininformatikinitiative.dataportal.backend.dse.api.DseProfile;
import de.medizininformatikinitiative.dataportal.backend.dse.api.Field;
import de.medizininformatikinitiative.dataportal.backend.dse.api.Reference;
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

  public UpgradeService(@NonNull DseService dseService) {
    this.dseService = dseService;
  }

  public UpgradeWrapper upgrade(@NonNull Crtdl crtdl) {
    var dataExtraction = crtdl.dataExtraction();
    if (dataExtraction == null || dataExtraction.attributeGroups() == null || dataExtraction.attributeGroups().isEmpty()) {
      return UpgradeWrapper.builder().crtdl(crtdl).annotations(List.of()).build();
    }

    var referencedGroups = dataExtraction.attributeGroups().stream()
        .map(AttributeGroup::groupReference).distinct().toList();
    var profileData = dseService.getProfileData(referencedGroups.stream().map(URI::toString).toList());

    var issues = new ArrayList<UpgradeIssue>();
    var fixedDataExtraction = dataExtraction;

    for (int i = 0; i < dataExtraction.attributeGroups().size(); i++) {
      var fixedAttributeGroup = dataExtraction.attributeGroups().get(i);
      AttributeGroup finalFixedAttributeGroup = fixedAttributeGroup;
      var dseProfile = profileData.stream()
          .filter(p -> finalFixedAttributeGroup.groupReference().toString().equals(p.url()))
          .findFirst()
          .orElseThrow(() -> new CrtdlUpgradeException("Dse Profile not found"));

      var handlers = dseProfile.errorCode() != null && !dseProfile.errorCode().isBlank()
          ? List.of((UpgradeHandler) this::handleProfileRemoved)
          : List.of(
          (UpgradeHandler) (de, ag, idx) -> handleFilterNameChange(de, ag, dseProfile, idx),
          (UpgradeHandler) (de, ag, idx) -> handleFieldOrReferenceRemoved(de, ag, dseProfile, idx),
          (UpgradeHandler) (de, ag, idx) -> handleLinkedGroupsRemoved(de, ag, dseProfile, idx)
      );

      for (var handler : handlers) {
        var result = handler.handle(fixedDataExtraction, fixedAttributeGroup, i);
        issues.addAll(result.upgradeIssues());
        fixedDataExtraction = result.dataExtraction();
        try {
          fixedAttributeGroup = findAttributeGroup(fixedDataExtraction, fixedAttributeGroup.id());
        } catch (NoSuchElementException e) {
          // If the group has been removed earlier, don't try to execute any other handlers on it
          break;
        }
      }
    }

    return UpgradeWrapper.builder()
        .crtdl(Crtdl.builder()
            .version(crtdl.version())
            .display(crtdl.display())
            .cohortDefinition(crtdl.cohortDefinition())
            .dataExtraction(fixedDataExtraction)
            .build())
        .annotations(issues)
        .build();
  }

  @FunctionalInterface
  private interface UpgradeHandler {
    DataExtractionUpgradeResult handle(DataExtraction dataExtraction, AttributeGroup attributeGroup, int groupIndex);
  }

  // --- Handlers ---

  private DataExtractionUpgradeResult handleProfileRemoved(DataExtraction dataExtraction, AttributeGroup attributeGroup, int groupIndex) {
    return DataExtractionUpgradeResult.builder()
        .dataExtraction(DataExtraction.withRemovedAttributeGroup(dataExtraction, attributeGroup))
        .upgradeIssues(List.of(buildGroupIssue(groupIndex, UpgradeIssueType.PROFILE_REMOVED, Map.of("replaced", attributeGroup))))
        .build();
  }

  private DataExtractionUpgradeResult handleFilterNameChange(DataExtraction dataExtraction, AttributeGroup attributeGroup, DseProfile dseProfile, int groupIndex) {
    var dateFilterCrtdl = attributeGroup.filter().stream().filter(f -> "date".equals(f.type())).findFirst();
    var dateFilterProfile = dseProfile.filters().stream().filter(f -> "date".equals(f.type())).findFirst();

    if (dateFilterCrtdl.isEmpty() || dateFilterProfile.isEmpty()) {
      return noChanges(dataExtraction);
    }

    var filterCrtdl = dateFilterCrtdl.get();
    var filterProfile = dateFilterProfile.get();
    if (filterCrtdl.name().equals(filterProfile.name())) {
      return noChanges(dataExtraction);
    }

    var fixedFilter = filterCrtdl.toBuilder().name(filterProfile.name()).build();
    int filterIndex = attributeGroup.filter().indexOf(filterCrtdl);

    return DataExtractionUpgradeResult.builder()
        .dataExtraction(DataExtraction.withReplacedDateFilter(dataExtraction, attributeGroup, fixedFilter))
        .upgradeIssues(List.of(buildAttributeIssue(groupIndex, filterIndex, UpgradeIssueType.FILTER_CHANGE,
            Map.of("replaced", filterCrtdl, "replacedWith", fixedFilter), "filter")))
        .build();
  }

  private DataExtractionUpgradeResult handleFieldOrReferenceRemoved(DataExtraction dataExtraction, AttributeGroup attributeGroup, DseProfile dseProfile, int groupIndex) {
    var fixedDataExtraction = dataExtraction;
    var fixedAttributeGroup = attributeGroup;
    var issues = new ArrayList<UpgradeIssue>();

    for (int attributeIndex = 0; attributeIndex < attributeGroup.attributes().size(); attributeIndex++) {
      var attribute = attributeGroup.attributes().get(attributeIndex);
      boolean hasLinkedGroups = attribute.linkedGroups() != null && !attribute.linkedGroups().isEmpty();

      boolean isAvailable = hasLinkedGroups
          ? dseProfile.references().stream().anyMatch(ref -> referenceOrChildrenMatch(ref, attribute.attributeRef()))
          : dseProfile.fields().stream().anyMatch(field -> fieldOrChildrenMatch(field, attribute.attributeRef()));

      if (isAvailable) continue;

      var firstMatch = hasLinkedGroups
          ? findFirstMatchingParentReference(attribute.attributeRef(), dseProfile)
          : findFirstMatchingParentField(attribute.attributeRef(), dseProfile);

      if (firstMatch.isPresent()) {
        var fixedAttribute = attribute.toBuilder().attributeRef(firstMatch.get()).build();
        boolean parentAlreadyPresent = fixedAttributeGroup.attributes().stream()
            .anyMatch(attr -> attr.attributeRef().equals(firstMatch.get()));

        fixedDataExtraction = parentAlreadyPresent
            ? DataExtraction.withRemovedAttribute(fixedDataExtraction, fixedAttributeGroup, attribute)
            : DataExtraction.withReplacedAttribute(fixedDataExtraction, fixedAttributeGroup, attribute, fixedAttribute);
        fixedAttributeGroup = findAttributeGroup(fixedDataExtraction, attributeGroup.id());

        var issueType = hasLinkedGroups ? UpgradeIssueType.REFERENCE_CHANGED_TO_PARENT : UpgradeIssueType.FIELD_CHANGED_TO_PARENT;
        issues.add(buildAttributeIssue(groupIndex, attributeIndex, issueType,
            Map.of("replaced", attribute, "replacedWith", fixedAttribute)));
      } else {
        fixedDataExtraction = DataExtraction.withRemovedAttribute(fixedDataExtraction, fixedAttributeGroup, attribute);
        fixedAttributeGroup = findAttributeGroup(fixedDataExtraction, fixedAttributeGroup.id());

        var issueType = hasLinkedGroups ? UpgradeIssueType.REFERENCE_NO_LONGER_AVAILABLE : UpgradeIssueType.FIELD_NO_LONGER_AVAILABLE;
        issues.add(buildAttributeIssue(groupIndex, attributeIndex, issueType, Map.of("replaced", attribute)));
      }
    }

    return DataExtractionUpgradeResult.builder()
        .dataExtraction(fixedDataExtraction)
        .upgradeIssues(issues)
        .build();
  }

  private DataExtractionUpgradeResult handleLinkedGroupsRemoved(DataExtraction dataExtraction, AttributeGroup attributeGroup, DseProfile dseProfile, int groupIndex) {
    var fixedDataExtraction = dataExtraction;
    var fixedAttributeGroup = attributeGroup;
    var issues = new ArrayList<UpgradeIssue>();

    for (int attributeIndex = 0; attributeIndex < attributeGroup.attributes().size(); attributeIndex++) {
      var attribute = attributeGroup.attributes().get(attributeIndex);
      if (attribute.linkedGroups() == null || attribute.linkedGroups().isEmpty()) continue;

      var fixedLinkedGroups = attribute.linkedGroups().stream()
          .filter(ref -> dataExtraction.attributeGroups().stream().anyMatch(ag -> ref.equals(ag.id())))
          .toList();

      if (fixedLinkedGroups.size() == attribute.linkedGroups().size()) continue;

      if (fixedLinkedGroups.isEmpty()) {
        fixedDataExtraction = DataExtraction.withRemovedAttribute(fixedDataExtraction, fixedAttributeGroup, attribute);
        fixedAttributeGroup = findAttributeGroup(fixedDataExtraction, attributeGroup.id());
        issues.add(buildAttributeIssue(groupIndex, attributeIndex, UpgradeIssueType.ALL_LINKED_GROUPS_NO_LONGER_AVAILABLE,
            Map.of("replaced", attribute)));
      } else {
        var fixedAttribute = Attribute.withReplacedLinkedGroups(attribute, fixedLinkedGroups);
        fixedDataExtraction = DataExtraction.withReplacedAttribute(fixedDataExtraction, fixedAttributeGroup, attribute, fixedAttribute);
        fixedAttributeGroup = findAttributeGroup(fixedDataExtraction, attributeGroup.id());
        issues.add(buildAttributeIssue(groupIndex, attributeIndex, UpgradeIssueType.LINKED_GROUPS_NO_LONGER_AVAILABLE,
            Map.of("replaced", attribute, "replacedWith", fixedAttribute)));
      }
    }

    return DataExtractionUpgradeResult.builder()
        .dataExtraction(fixedDataExtraction)
        .upgradeIssues(issues)
        .build();
  }

  // --- Issue builders ---

  private UpgradeIssue buildGroupIssue(int groupIndex, UpgradeIssueType type, Map<String, Object> details) {
    return buildIssue(MessageFormat.format("dataExtraction/attributeGroups/{0}", groupIndex), type, details);
  }

  private UpgradeIssue buildAttributeIssue(int groupIndex, int attributeIndex, UpgradeIssueType type, Map<String, Object> details) {
    return buildAttributeIssue(groupIndex, attributeIndex, type, details, "attributes");
  }

  private UpgradeIssue buildAttributeIssue(int groupIndex, int itemIndex, UpgradeIssueType type, Map<String, Object> details, String segment) {
    return buildIssue(MessageFormat.format("dataExtraction/attributeGroups/{0}/{1}/{2}", groupIndex, segment, itemIndex), type, details);
  }

  private UpgradeIssue buildIssue(String path, UpgradeIssueType type, Map<String, Object> details) {
    return UpgradeIssue.builder()
        .path(path)
        .value(UpgradeIssueValue.builder()
            .message(type.detail())
            .code("UPGRADE-" + type.code())
            .build())
        .details(details)
        .build();
  }

  // --- DSE matching helpers ---

  public static Optional<String> findFirstMatchingParentField(String attributeRef, DseProfile dseProfile) {
    return findFirstMatchingParent(attributeRef, id ->
        dseProfile.fields().stream().anyMatch(field -> field.id().equals(id)));
  }

  public static Optional<String> findFirstMatchingParentReference(String attributeRef, DseProfile dseProfile) {
    return findFirstMatchingParent(attributeRef, id ->
        dseProfile.references().stream().anyMatch(ref -> ref.id().equals(id)));
  }

  private static Optional<String> findFirstMatchingParent(String attributeRef, java.util.function.Predicate<String> matcher) {
    var current = attributeRef;
    while (true) {
      if (matcher.test(current)) return Optional.of(current);
      int lastDot = current.lastIndexOf('.');
      if (lastDot < 0) return Optional.empty();
      current = current.substring(0, lastDot);
    }
  }

  private boolean fieldOrChildrenMatch(Field field, String attributeRef) {
    if (field.id().equalsIgnoreCase(attributeRef)) return true;
    return field.children() != null && field.children().stream()
        .anyMatch(child -> fieldOrChildrenMatch(child, attributeRef));
  }

  private boolean referenceOrChildrenMatch(Reference reference, String attributeRef) {
    if (reference.id().equalsIgnoreCase(attributeRef)) return true;
    return reference.children() != null && reference.children().stream()
        .anyMatch(child -> referenceOrChildrenMatch(child, attributeRef));
  }

  private AttributeGroup findAttributeGroup(DataExtraction extraction, String id) {
    return extraction.attributeGroups().stream()
        .filter(g -> Objects.equals(g.id(), id))
        .findFirst()
        .orElseThrow();
  }

  private static DataExtractionUpgradeResult noChanges(DataExtraction dataExtraction) {
    return DataExtractionUpgradeResult.builder()
        .dataExtraction(dataExtraction)
        .upgradeIssues(List.of())
        .build();
  }
}
