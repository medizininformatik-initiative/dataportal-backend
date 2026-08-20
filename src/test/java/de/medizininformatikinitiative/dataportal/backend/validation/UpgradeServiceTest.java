package de.medizininformatikinitiative.dataportal.backend.validation;

import de.medizininformatikinitiative.dataportal.backend.dse.DseService;
import de.medizininformatikinitiative.dataportal.backend.dse.api.DseProfile;
import de.medizininformatikinitiative.dataportal.backend.dse.api.Field;
import de.medizininformatikinitiative.dataportal.backend.dse.api.Reference;
import de.medizininformatikinitiative.dataportal.backend.query.api.Attribute;
import de.medizininformatikinitiative.dataportal.backend.query.api.AttributeGroup;
import de.medizininformatikinitiative.dataportal.backend.query.api.Crtdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.DataExtraction;
import de.medizininformatikinitiative.dataportal.backend.query.api.Filter;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.UpgradeIssueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class UpgradeServiceTest {

  private static final URI GROUP_1_REF = URI.create("https://example.org/StructureDefinition/group-1");
  private static final URI GROUP_2_REF = URI.create("https://example.org/StructureDefinition/group-2");

  @Mock
  private DseService dseService;

  private UpgradeService upgradeService;

  @BeforeEach
  void setUp() {
    upgradeService = new UpgradeService(dseService);
  }

  @Test
  void upgrade_throwsOnNullCrtdl() {
    assertThrows(NullPointerException.class, () -> upgradeService.upgrade(null));
  }

  @Test
  void upgrade_nullDataExtraction_returnsUnchangedNoAnnotations() {
    var crtdl = crtdl(null);

    var result = upgradeService.upgrade(crtdl);

    assertThat(result.annotations()).isEmpty();
    assertThat(result.crtdl()).isEqualTo(crtdl);
  }

  @Test
  void upgrade_emptyAttributeGroups_returnsUnchangedNoAnnotations() {
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of()).build());

    var result = upgradeService.upgrade(crtdl);

    assertThat(result.annotations()).isEmpty();
    assertThat(result.crtdl()).isEqualTo(crtdl);
  }

  @Test
  void upgrade_profileNotFoundInProfileData_throws() {
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of());
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(group)).build());
    doReturn(List.of()).when(dseService).getProfileData(anyList());

    assertThrows(CrtdlUpgradeException.class, () -> upgradeService.upgrade(crtdl));
  }

  @Test
  void upgrade_profileRemoved_removesGroupAndAddsIssue() {
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of());
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(group)).build());
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .errorCode("TBD-00000").errorCause("profile not found").build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    assertThat(result.crtdl().dataExtraction().attributeGroups()).isEmpty();
    assertThat(result.annotations()).hasSize(1);
    var issue = result.annotations().get(0);
    assertThat(issue.value().code()).isEqualTo("UPGRADE-" + UpgradeIssueType.PROFILE_REMOVED.code());
    assertThat(issue.path()).isEqualTo("dataExtraction/attributeGroups/0");
  }

  @Test
  void upgrade_dateFilterNameChanged_replacesFilterNameAndAddsIssue() {
    var oldFilter = Filter.builder().type("date").name("recorded-date").build();
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of(oldFilter));
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(group)).build());

    var field = Field.builder().id("Patient.active").build();
    var newDseFilter = de.medizininformatikinitiative.dataportal.backend.dse.api.Filter.builder()
        .type("date").name("date-recorded").build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(field)).filters(List.of(newDseFilter)).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    var resultGroup = result.crtdl().dataExtraction().attributeGroups().get(0);
    assertThat(resultGroup.filter()).hasSize(1);
    assertThat(resultGroup.filter().get(0).name()).isEqualTo("date-recorded");
    assertThat(result.annotations()).hasSize(1);
    var issue = result.annotations().get(0);
    assertThat(issue.value().code()).isEqualTo("UPGRADE-" + UpgradeIssueType.FILTER_CHANGE.code());
    assertThat(issue.path()).isEqualTo("dataExtraction/attributeGroups/0/filter/0");
  }

  @Test
  void upgrade_dateFilterSameName_noChangeNoIssue() {
    var filter = Filter.builder().type("date").name("date-recorded").build();
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of(filter));
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(group)).build());

    var field = Field.builder().id("Patient.active").build();
    var dseFilter = de.medizininformatikinitiative.dataportal.backend.dse.api.Filter.builder()
        .type("date").name("date-recorded").build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(field)).filters(List.of(dseFilter)).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    assertThat(result.annotations()).isEmpty();
    assertThat(result.crtdl().dataExtraction().attributeGroups().get(0).filter().get(0).name())
        .isEqualTo("date-recorded");
  }

  @Test
  void upgrade_fieldNoLongerAvailable_removesAttributeAndAddsIssue() {
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.removedField")), List.of());
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(group)).build());

    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of()).filters(List.of()).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    assertThat(result.crtdl().dataExtraction().attributeGroups().get(0).attributes()).isEmpty();
    assertThat(result.annotations()).hasSize(1);
    var issue = result.annotations().get(0);
    assertThat(issue.value().code()).isEqualTo("UPGRADE-" + UpgradeIssueType.FIELD_NO_LONGER_AVAILABLE.code());
    assertThat(issue.path()).isEqualTo("dataExtraction/attributeGroups/0/attributes/0");
  }

  @Test
  void upgrade_fieldChangedToParent_promotesAttributeRefAndAddsIssue() {
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.address.country")), List.of());
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(group)).build());

    var field = Field.builder().id("Patient.address").build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(field)).filters(List.of()).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    var attrs = result.crtdl().dataExtraction().attributeGroups().get(0).attributes();
    assertThat(attrs).hasSize(1);
    assertThat(attrs.get(0).attributeRef()).isEqualTo("Patient.address");
    assertThat(result.annotations()).hasSize(1);
    assertThat(result.annotations().get(0).value().code())
        .isEqualTo("UPGRADE-" + UpgradeIssueType.FIELD_CHANGED_TO_PARENT.code());
  }

  @Test
  void upgrade_fieldChangedToParentAlreadyPresent_removesDuplicateAttribute() {
    var group = attributeGroup("group-1", GROUP_1_REF,
        List.of(attribute("Patient.address"), attribute("Patient.address.country")), List.of());
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(group)).build());

    var field = Field.builder().id("Patient.address").build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(field)).filters(List.of()).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    var attrs = result.crtdl().dataExtraction().attributeGroups().get(0).attributes();
    assertThat(attrs).hasSize(1);
    assertThat(attrs.get(0).attributeRef()).isEqualTo("Patient.address");
    assertThat(result.annotations()).hasSize(1);
    assertThat(result.annotations().get(0).value().code())
        .isEqualTo("UPGRADE-" + UpgradeIssueType.FIELD_CHANGED_TO_PARENT.code());
  }

  @Test
  void upgrade_fieldMatchesNestedChild_noChangeNoIssue() {
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.address.country")), List.of());
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(group)).build());

    var childField = Field.builder().id("Patient.address.country").build();
    var parentField = Field.builder().id("Patient.address").children(List.of(childField)).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(parentField)).filters(List.of()).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    assertThat(result.annotations()).isEmpty();
    assertThat(result.crtdl().dataExtraction().attributeGroups().get(0).attributes().get(0).attributeRef())
        .isEqualTo("Patient.address.country");
  }

  @Test
  void upgrade_referenceNoLongerAvailable_removesAttributeAndAddsIssue() {
    var attr = attributeWithLinkedGroups("Observation.removedRef", List.of("group-1"));
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attr), List.of());
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(group)).build());

    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of()).filters(List.of()).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    assertThat(result.crtdl().dataExtraction().attributeGroups().get(0).attributes()).isEmpty();
    assertThat(result.annotations()).hasSize(1);
    assertThat(result.annotations().get(0).value().code())
        .isEqualTo("UPGRADE-" + UpgradeIssueType.REFERENCE_NO_LONGER_AVAILABLE.code());
  }

  @Test
  void upgrade_referenceChangedToParent_promotesAttributeRefAndAddsIssue() {
    var attr = attributeWithLinkedGroups("Observation.subject.reference", List.of("group-1"));
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attr), List.of());
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(group)).build());

    var reference = Reference.builder().id("Observation.subject").build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of()).filters(List.of()).references(List.of(reference)).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    var attrs = result.crtdl().dataExtraction().attributeGroups().get(0).attributes();
    assertThat(attrs).hasSize(1);
    assertThat(attrs.get(0).attributeRef()).isEqualTo("Observation.subject");
    assertThat(result.annotations()).hasSize(1);
    assertThat(result.annotations().get(0).value().code())
        .isEqualTo("UPGRADE-" + UpgradeIssueType.REFERENCE_CHANGED_TO_PARENT.code());
  }

  @Test
  void upgrade_linkedGroupPartiallyMissing_prunesMissingLinkAndAddsIssue() {
    var attr = attributeWithLinkedGroups("Observation.subject.reference", List.of("group-1", "group-missing"));
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attr), List.of());
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(group)).build());

    var reference = Reference.builder().id("Observation.subject.reference").build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of()).filters(List.of()).references(List.of(reference)).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    var attrs = result.crtdl().dataExtraction().attributeGroups().get(0).attributes();
    assertThat(attrs).hasSize(1);
    assertThat(attrs.get(0).linkedGroups()).containsExactly("group-1");
    assertThat(result.annotations()).hasSize(1);
    assertThat(result.annotations().get(0).value().code())
        .isEqualTo("UPGRADE-" + UpgradeIssueType.LINKED_GROUPS_NO_LONGER_AVAILABLE.code());
  }

  @Test
  void upgrade_allLinkedGroupsMissing_removesAttributeAndAddsIssue() {
    var attr = attributeWithLinkedGroups("Observation.subject.reference", List.of("group-missing"));
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attr), List.of());
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(group)).build());

    var reference = Reference.builder().id("Observation.subject.reference").build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of()).filters(List.of()).references(List.of(reference)).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    assertThat(result.crtdl().dataExtraction().attributeGroups().get(0).attributes()).isEmpty();
    assertThat(result.annotations()).hasSize(1);
    assertThat(result.annotations().get(0).value().code())
        .isEqualTo("UPGRADE-" + UpgradeIssueType.ALL_LINKED_GROUPS_NO_LONGER_AVAILABLE.code());
  }

  @Test
  void upgrade_preservesVersionDisplayAndCohortDefinition() {
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of());
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var crtdl = Crtdl.builder().version("1.0.0").display("Test CRTDL").cohortDefinition(null)
        .dataExtraction(dataExtraction).build();

    var field = Field.builder().id("Patient.active").build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(field)).filters(List.of()).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    assertThat(result.crtdl().version()).isEqualTo("1.0.0");
    assertThat(result.crtdl().display()).isEqualTo("Test CRTDL");
    assertThat(result.crtdl().cohortDefinition()).isNull();
  }

  @Test
  void upgrade_multipleGroups_aggregatesIssuesAcrossGroups() {
    var groupOk = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of());
    var groupRemoved = attributeGroup("group-2", GROUP_2_REF, List.of(attribute("Observation.value")), List.of());
    var crtdl = crtdl(DataExtraction.builder().attributeGroups(List.of(groupOk, groupRemoved)).build());

    var okField = Field.builder().id("Patient.active").build();
    var okProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(okField)).filters(List.of()).references(List.of()).build();
    var removedProfile = DseProfile.builder().url(GROUP_2_REF.toString())
        .errorCode("TBD-00000").errorCause("profile not found").build();
    doReturn(List.of(okProfile, removedProfile)).when(dseService).getProfileData(anyList());

    var result = upgradeService.upgrade(crtdl);

    var groups = result.crtdl().dataExtraction().attributeGroups();
    assertThat(groups).hasSize(1);
    assertThat(groups.get(0).id()).isEqualTo("group-1");
    assertThat(result.annotations()).hasSize(1);
    assertThat(result.annotations().get(0).value().code())
        .isEqualTo("UPGRADE-" + UpgradeIssueType.PROFILE_REMOVED.code());
  }

  @Test
  void findFirstMatchingParentField_returnsExactMatchWhenPresent() {
    var dseProfile = DseProfile.builder()
        .fields(List.of(Field.builder().id("Patient.address").build())).build();

    assertThat(UpgradeService.findFirstMatchingParentField("Patient.address", dseProfile))
        .contains("Patient.address");
  }

  @Test
  void findFirstMatchingParentField_walksUpToClosestAncestor() {
    var dseProfile = DseProfile.builder()
        .fields(List.of(Field.builder().id("Patient").build())).build();

    assertThat(UpgradeService.findFirstMatchingParentField("Patient.address.country", dseProfile))
        .contains("Patient");
  }

  @Test
  void findFirstMatchingParentField_returnsEmptyWhenNoAncestorMatches() {
    var dseProfile = DseProfile.builder().fields(List.of()).build();

    assertThat(UpgradeService.findFirstMatchingParentField("Patient.address.country", dseProfile))
        .isEmpty();
  }

  @Test
  void findFirstMatchingParentReference_walksUpToClosestAncestor() {
    var dseProfile = DseProfile.builder()
        .references(List.of(Reference.builder().id("Observation.subject").build())).build();

    assertThat(UpgradeService.findFirstMatchingParentReference("Observation.subject.reference", dseProfile))
        .contains("Observation.subject");
  }

  // --- fixtures ---

  private Crtdl crtdl(DataExtraction dataExtraction) {
    return Crtdl.builder().version("1.0.0").display("Test CRTDL").dataExtraction(dataExtraction).build();
  }

  private AttributeGroup attributeGroup(String id, URI groupReference, List<Attribute> attributes, List<Filter> filters) {
    return AttributeGroup.builder().id(id).groupReference(groupReference).attributes(attributes).filter(filters).build();
  }

  private Attribute attribute(String ref) {
    return Attribute.builder().attributeRef(ref).mustHave(false).build();
  }

  private Attribute attributeWithLinkedGroups(String ref, List<String> linkedGroups) {
    return Attribute.builder().attributeRef(ref).mustHave(false).linkedGroups(linkedGroups).build();
  }
}
