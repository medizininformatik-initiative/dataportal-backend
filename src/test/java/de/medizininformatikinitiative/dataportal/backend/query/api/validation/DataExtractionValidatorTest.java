package de.medizininformatikinitiative.dataportal.backend.query.api.validation;

import de.medizininformatikinitiative.dataportal.backend.common.api.TermCode;
import de.medizininformatikinitiative.dataportal.backend.dse.DseService;
import de.medizininformatikinitiative.dataportal.backend.dse.api.DseProfile;
import de.medizininformatikinitiative.dataportal.backend.dse.api.Field;
import de.medizininformatikinitiative.dataportal.backend.dse.api.Reference;
import de.medizininformatikinitiative.dataportal.backend.query.api.Attribute;
import de.medizininformatikinitiative.dataportal.backend.query.api.AttributeGroup;
import de.medizininformatikinitiative.dataportal.backend.query.api.DataExtraction;
import de.medizininformatikinitiative.dataportal.backend.query.api.Filter;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssueType;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.CodeableConceptService;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Tag("query")
@Tag("api")
@Tag("validation")
@ExtendWith(MockitoExtension.class)
class DataExtractionValidatorTest {

  private static final URI GROUP_1_REF = URI.create("https://example.org/StructureDefinition/group-1");

  @Mock
  private CodeableConceptService codeableConceptService;

  @Mock
  private DseService dseService;

  @Mock
  private ConstraintValidatorContext ctx;

  @Mock
  private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

  private DataExtractionValidator validator;

  @BeforeEach
  void setUp() {
    lenient().when(ctx.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    lenient().when(violationBuilder.addConstraintViolation()).thenReturn(ctx);
    validator = new DataExtractionValidator(codeableConceptService, dseService, JsonMapper.builderWithJackson2Defaults().build());
  }

  @Test
  void isValid_nullDataExtraction_true() {
    assertTrue(validator.isValid(null, ctx));
  }

  @Test
  void isValid_emptyAttributeGroups_true() {
    assertTrue(validator.isValid(DataExtraction.builder().attributeGroups(List.of()).build(), ctx));
  }

  @Test
  void isValid_profileNotFound_false() {
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of());
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    doReturn(List.of()).when(dseService).getProfileData(anyList());

    var result = validator.isValid(dataExtraction, ctx);

    assertFalse(result);
    var captor = ArgumentCaptor.forClass(String.class);
    verify(ctx).buildConstraintViolationWithTemplate(captor.capture());
    assertTemplateHasCode(captor.getValue(), ValidationIssueType.ATTRIBUTE_GROUP_PROFILE_NOT_FOUND);
  }

  @Test
  void isValid_profileHasErrorCode_false() {
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of());
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString()).errorCode("TBD-00000").build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    assertFalse(validator.isValid(dataExtraction, ctx));
    verify(ctx).buildConstraintViolationWithTemplate(anyString());
  }

  @Test
  void isValid_attributeRefNotFoundInFieldsOrReferences_false() {
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.unknown")), List.of());
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of()).filters(List.of()).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = validator.isValid(dataExtraction, ctx);

    assertFalse(result);
    var captor = ArgumentCaptor.forClass(String.class);
    verify(ctx).buildConstraintViolationWithTemplate(captor.capture());
    assertTemplateHasCode(captor.getValue(), ValidationIssueType.ATTRIBUTE_REF_NOT_FOUND);
  }

  @Test
  void isValid_attributeRefMatchesFieldExactly_true() {
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of());
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(Field.builder().id("Patient.active").build()))
        .filters(List.of()).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    assertTrue(validator.isValid(dataExtraction, ctx));
    verify(ctx, never()).buildConstraintViolationWithTemplate(anyString());
  }

  @Test
  void isValid_attributeRefMatchesNestedChildField_true() {
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.address.country")), List.of());
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var childField = Field.builder().id("Patient.address.country").build();
    var parentField = Field.builder().id("Patient.address").children(List.of(childField)).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(parentField)).filters(List.of()).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    assertTrue(validator.isValid(dataExtraction, ctx));
  }

  @Test
  void isValid_referenceAttributeWithoutLinkedGroups_false() {
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Observation.subject")), List.of());
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of()).filters(List.of())
        .references(List.of(Reference.builder().id("Observation.subject").build())).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = validator.isValid(dataExtraction, ctx);

    assertFalse(result);
    var captor = ArgumentCaptor.forClass(String.class);
    verify(ctx).buildConstraintViolationWithTemplate(captor.capture());
    assertTemplateHasCode(captor.getValue(), ValidationIssueType.LINKED_GROUP_MISSING);
  }

  @Test
  void isValid_referenceAttributeWithLinkedGroups_true() {
    var linked = attributeWithLinkedGroups("Observation.subject", List.of("group-1"));
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(linked), List.of());
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of()).filters(List.of())
        .references(List.of(Reference.builder().id("Observation.subject").build())).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    assertTrue(validator.isValid(dataExtraction, ctx));
  }

  @Test
  void isValid_linkedGroupNotFound_false() {
    var linked = attributeWithLinkedGroups("Observation.subject", List.of("group-missing"));
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(linked), List.of());
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of()).filters(List.of())
        .references(List.of(Reference.builder().id("Observation.subject").build())).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = validator.isValid(dataExtraction, ctx);

    assertFalse(result);
    var captor = ArgumentCaptor.forClass(String.class);
    verify(ctx).buildConstraintViolationWithTemplate(captor.capture());
    assertTemplateHasCode(captor.getValue(), ValidationIssueType.LINKED_GROUP_NOT_FOUND);
  }

  @Test
  void isValid_unsupportedFilterType_false() {
    var filter = Filter.builder().type("token").name("code").codes(List.of()).build();
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of(filter));
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(Field.builder().id("Patient.active").build()))
        .filters(List.of(de.medizininformatikinitiative.dataportal.backend.dse.api.Filter.builder().type("date").name("date").build()))
        .references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = validator.isValid(dataExtraction, ctx);

    assertFalse(result);
    var captor = ArgumentCaptor.forClass(String.class);
    verify(ctx).buildConstraintViolationWithTemplate(captor.capture());
    assertTemplateHasCode(captor.getValue(), ValidationIssueType.FILTER_TYPE_NOT_SUPPORTED);
  }

  @Test
  void isValid_filterCodeMissingInValueSet_false() {
    var termCode = TermCode.builder().code("123").system("http://example.org").display("Foo").version("1.0").build();
    var filter = Filter.builder().type("token").name("code").codes(List.of(termCode)).build();
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of(filter));
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var dseFilter = de.medizininformatikinitiative.dataportal.backend.dse.api.Filter.builder()
        .type("token").name("code").valueSetUrls(List.of("http://example.org/ValueSet/foo")).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(Field.builder().id("Patient.active").build()))
        .filters(List.of(dseFilter)).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());
    doReturn(List.of()).when(codeableConceptService).availableCodesInValueSets(anyList(), anyList());

    var result = validator.isValid(dataExtraction, ctx);

    assertFalse(result);
    var captor = ArgumentCaptor.forClass(String.class);
    verify(ctx).buildConstraintViolationWithTemplate(captor.capture());
    assertTemplateHasCode(captor.getValue(), ValidationIssueType.FILTER_CODE_NOT_FOUND);
  }

  @Test
  void isValid_filterCodeAvailableInValueSet_true() {
    var termCode = TermCode.builder().code("123").system("http://example.org").display("Foo").build();
    var filter = Filter.builder().type("token").name("code").codes(List.of(termCode)).build();
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of(filter));
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var dseFilter = de.medizininformatikinitiative.dataportal.backend.dse.api.Filter.builder()
        .type("token").name("code").valueSetUrls(List.of("http://example.org/ValueSet/foo")).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(Field.builder().id("Patient.active").build()))
        .filters(List.of(dseFilter)).references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());
    doReturn(List.of("123")).when(codeableConceptService).availableCodesInValueSets(anyList(), anyList());

    assertTrue(validator.isValid(dataExtraction, ctx));
  }

  @Test
  void isValid_dateFilterEndBeforeStart_false() {
    var filter = Filter.builder().type("date").name("date").start(LocalDate.of(2025, 10, 18)).end(LocalDate.of(2025, 10, 14)).build();
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of(filter));
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(Field.builder().id("Patient.active").build()))
        .filters(List.of(de.medizininformatikinitiative.dataportal.backend.dse.api.Filter.builder().type("date").name("date").build()))
        .references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    var result = validator.isValid(dataExtraction, ctx);

    assertFalse(result);
    var captor = ArgumentCaptor.forClass(String.class);
    verify(ctx).buildConstraintViolationWithTemplate(captor.capture());
    assertTemplateHasCode(captor.getValue(), ValidationIssueType.FILTER_DATE_COMBINATION_INVALID);
  }

  @Test
  void isValid_dateFilterStartBeforeEnd_true() {
    var filter = Filter.builder().type("date").name("date").start(LocalDate.of(2025, 10, 14)).end(LocalDate.of(2025, 10, 18)).build();
    var group = attributeGroup("group-1", GROUP_1_REF, List.of(attribute("Patient.active")), List.of(filter));
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(Field.builder().id("Patient.active").build()))
        .filters(List.of(de.medizininformatikinitiative.dataportal.backend.dse.api.Filter.builder().type("date").name("date").build()))
        .references(List.of()).build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    assertTrue(validator.isValid(dataExtraction, ctx));
  }

  @Test
  void isValid_fullyValidDataExtraction_true() {
    var linked = attributeWithLinkedGroups("Observation.subject", List.of("group-1"));
    var filter = Filter.builder().type("date").name("date").start(LocalDate.of(2025, 1, 1)).end(LocalDate.of(2025, 1, 2)).build();
    var group = attributeGroup("group-1", GROUP_1_REF,
        List.of(attribute("Patient.active"), linked), List.of(filter));
    var dataExtraction = DataExtraction.builder().attributeGroups(List.of(group)).build();
    var dseProfile = DseProfile.builder().url(GROUP_1_REF.toString())
        .fields(List.of(Field.builder().id("Patient.active").build()))
        .filters(List.of(de.medizininformatikinitiative.dataportal.backend.dse.api.Filter.builder().type("date").name("date").build()))
        .references(List.of(Reference.builder().id("Observation.subject").build()))
        .build();
    doReturn(List.of(dseProfile)).when(dseService).getProfileData(anyList());

    assertTrue(validator.isValid(dataExtraction, ctx));
    verify(ctx, never()).buildConstraintViolationWithTemplate(anyString());
  }

  private void assertTemplateHasCode(String template, ValidationIssueType type) {
    assertTrue(template.contains("VALIDATION-" + type.code()),
        "expected template to contain code VALIDATION-" + type.code() + " but was: " + template);
  }

  // --- fixtures ---

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
