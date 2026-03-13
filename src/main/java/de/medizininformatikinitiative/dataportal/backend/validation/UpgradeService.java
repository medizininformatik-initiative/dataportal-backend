package de.medizininformatikinitiative.dataportal.backend.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.medizininformatikinitiative.dataportal.backend.dse.DseService;
import de.medizininformatikinitiative.dataportal.backend.dse.api.DseProfile;
import de.medizininformatikinitiative.dataportal.backend.dse.api.Field;
import de.medizininformatikinitiative.dataportal.backend.query.api.*;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.UpgradeIssue;
import de.medizininformatikinitiative.dataportal.backend.validation.api.UpgradeWrapper;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Optional;

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
      Pair<DataExtraction, UpgradeIssue> filterNameChangeResult = handleFilterNameChange(crtdl.dataExtraction(), attributeGroup, dseProfile);


      // UPGRADE-1000002: Field removed as no longer available + UPGRADE-1000003: Field changed to parent
      for (Attribute a : attributeGroup.attributes()) {
        if (dseProfile.fields().stream().noneMatch(field -> fieldOrChildrenMatch(field, a.attributeRef()))) {
          System.out.println("not matching: " + a.attributeRef());
          var firstMatch = findFirstMatchingParent(a.attributeRef(), dseProfile);
          if (firstMatch.isPresent()) {
            System.out.println("found existing parent: " + firstMatch.get());
          } else {
            System.out.println("no matching parent found. removing");
          }
         }
      }

      // UPGRADE-1000004: Profile removed as it does not exist anymore

    }


    return UpgradeWrapper.builder()
        .crtdl(Crtdl.builder()
            .version(crtdl.version())
            .display(crtdl.display())
            .cohortDefinition(crtdl.cohortDefinition())
            .dataExtraction(crtdl.dataExtraction())
            .build())
        .annotations(List.of())
        .build();
  }

  private Pair<DataExtraction, UpgradeIssue> handleFilterNameChange(DataExtraction dataExtraction, AttributeGroup attributeGroup, DseProfile dseProfile) {
    // Right now there should be only one date filter, and that is the only thing we can automatically fix here
    Optional<Filter> dateFilterCrtdl = attributeGroup.filter().stream().filter(f -> "date".equals(f.type())).findFirst();
    Optional<de.medizininformatikinitiative.dataportal.backend.dse.api.Filter> dateFilterProfile = dseProfile.filters().stream().filter(f -> "date".equals(f.type())).findFirst();

    if (dateFilterCrtdl.isPresent() && dateFilterProfile.isPresent()) {
      var filterCrtdl = dateFilterCrtdl.get();
      var filterProfile = dateFilterProfile.get();

      if (!filterCrtdl.name().equals(filterProfile.name())) {

      }
    }
  }

  public static Optional<String> findFirstMatchingParent(String attributeRef, DseProfile dseProfile) {
    String current = attributeRef;

    while (true) {
      if (dseProfile.fields().stream().anyMatch(field -> field.id().equals(attributeRef))) {
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
}
