package de.numcodex.feasibility_gui_backend.query.dataquery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.*;
import de.numcodex.feasibility_gui_backend.dse.api.LocalizedValue;
import de.numcodex.feasibility_gui_backend.dse.persistence.DseProfile;
import de.numcodex.feasibility_gui_backend.dse.persistence.DseProfileRepository;
import de.numcodex.feasibility_gui_backend.query.api.*;
import de.numcodex.feasibility_gui_backend.terminology.api.CodeableConceptEntry;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.CodeableConceptService;
import de.numcodex.feasibility_gui_backend.terminology.es.TerminologyEsService;
import de.numcodex.feasibility_gui_backend.terminology.persistence.UiProfile;
import de.numcodex.feasibility_gui_backend.terminology.persistence.UiProfileRepository;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataqueryCsvExportServiceTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private DseProfileRepository dseProfileRepository;

  @Mock
  private UiProfileRepository uiProfileRepository;

  @Mock
  private TerminologyEsService terminologyEsService;

  @Mock
  private CodeableConceptService codeableConceptService;

  @InjectMocks
  private DataqueryCsvExportService dataqueryCsvExportService;

  @ParameterizedTest
  @MethodSource("provideParamsForCriteriaTest")
  void testJsonToCsv_criteria(DataqueryCsvExportService.SUPPORTED_LANGUAGES language,
                              ValueFilterType valueFilterType,
                              boolean includeTimerestriction,
                              boolean withAfterDate,
                              boolean withBeforeDate) throws IOException {
    var ccdl = createValidCcdl(valueFilterType, includeTimerestriction, withAfterDate, withBeforeDate);
    DisplayEntry displayEntry = createDisplayEntry();

    EsSearchResultEntry esSearchResultEntry = mock(EsSearchResultEntry.class);
    doReturn(displayEntry).when(esSearchResultEntry).display();
    doReturn(esSearchResultEntry).when(terminologyEsService).getSearchResultEntryByCriterion(any(Criterion.class));
    doReturn(Optional.of(createUiProfile())).when(uiProfileRepository).findByContextualizedTermcodeHash(any(String.class));
    doReturn(createUiProfileApi()).when(objectMapper).readValue(anyString(), eq(de.numcodex.feasibility_gui_backend.terminology.api.UiProfile.class));

    String csvResult = dataqueryCsvExportService.jsonToCsv(ccdl.inclusionCriteria(), language);

    assertNotNull(csvResult);
    switch (language) {
      case EN:
        assertTrue(csvResult.contains(displayEntry.translations().get(1).value()));
        break;
      case DE:
        assertTrue(csvResult.contains(displayEntry.translations().get(0).value()));
         break;
    }
  }

  @ParameterizedTest
  @EnumSource
  void testJsonToCsv_dataExtraction(DataqueryCsvExportService.SUPPORTED_LANGUAGES language) throws IOException {
    var dataExtraction = createValidDataExtraction();
    DisplayEntry displayEntry = createDisplayEntry();

    CodeableConceptEntry ccEntry = mock(CodeableConceptEntry.class);
    doReturn(displayEntry).when(ccEntry).display();
    doReturn(ccEntry).when(codeableConceptService).getSearchResultEntryByTermCode(any(TermCode.class));
    doReturn(Optional.of(createDseProfile())).when(dseProfileRepository).findByUrl(anyString());
    doReturn(createDseProfileApi()).when(objectMapper).readValue(anyString(), eq(de.numcodex.feasibility_gui_backend.dse.api.DseProfile.class));

    String csvResult = dataqueryCsvExportService.jsonToCsv(dataExtraction, language);

    assertNotNull(csvResult);
    assertTrue(csvResult.contains("basic testgroup"));
  }

  @Test
  void testAddFileToZip() throws IOException {
    String fileName = "dataquery.json";
    String fileContent = """
        {
            "foo": "bar",
            "baz": false,
            "foobar": 42
        }
        """;

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      dataqueryCsvExportService.addFileToZip(zos, fileName, fileContent);
    }

    try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
         ZipInputStream zis = new ZipInputStream(bais)) {

      ZipEntry entry = zis.getNextEntry();
      assertNotNull(entry, "zipfile is empty");
      assertEquals(fileName, entry.getName(), "wrong filename inside zip archive");

      byte[] buffer = new byte[1024];
      int len = zis.read(buffer);
      String extractedContent = new String(buffer, 0, len);
      assertEquals(fileContent, extractedContent, "zip content does not match");

      assertNull(zis.getNextEntry(), "too many entries in zipfile");
    }
  }

  private static Stream<Arguments> provideParamsForCriteriaTest() {
    List<Arguments> argumentsList = new ArrayList<>();
    for (DataqueryCsvExportService.SUPPORTED_LANGUAGES language : DataqueryCsvExportService.SUPPORTED_LANGUAGES.values()) {
      for (ValueFilterType valueFilterType : ValueFilterType.values()) {
        argumentsList.add(Arguments.of(language, valueFilterType, true, true, true));
        argumentsList.add(Arguments.of(language, valueFilterType, true, true, false));
        argumentsList.add(Arguments.of(language, valueFilterType, true, false, true));
        argumentsList.add(Arguments.of(language, valueFilterType, true, false, false));
        argumentsList.add(Arguments.of(language, valueFilterType, false, false, false));
        // When timerestriction is not included, there is no need to check different before/after date settings
      }
    }
    return Stream.of(argumentsList.toArray(Arguments[]::new));

  }

  private DataExtraction createValidDataExtraction() {
    return DataExtraction.builder()
        .attributeGroups(createAttributeGroups())
        .build();
  }

  private List<AttributeGroup> createAttributeGroups() {
    var referenceId = "my-referenced-group";
    return List.of(
        AttributeGroup.builder()
        .groupReference(URI.create("https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab"))
        .name("basic testgroup")
        .id("my-grp")
        .attributes(List.of(
            Attribute.builder().attributeRef("Observation.identifier").build(),
            Attribute.builder().attributeRef("Observation.status").build(),
            Attribute.builder().attributeRef("Observation.category").build(),
            Attribute.builder().attributeRef("Observation.code").build(),
            Attribute.builder().attributeRef("Observation.effective[x]").build()
        ))
        .build(),

        AttributeGroup.builder()
            .groupReference(URI.create("https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab"))
            .name("referencing testgroup")
            .id("my-referencing-grp")
            .attributes(List.of(
                Attribute.builder().attributeRef("Observation.identifier").build(),
                Attribute.builder().attributeRef("Observation.status").build(),
                Attribute.builder()
                    .attributeRef("Observation.category")
                    .linkedGroups(List.of(referenceId))
                    .build(),
                Attribute.builder().attributeRef("Observation.code").build(),
                Attribute.builder().attributeRef("Observation.effective[x]").build()
            ))
            .filter(
                List.of(
                    Filter.builder()
                        .type("date")
                        .name("date")
                        .start(LocalDate.of(2020, 1, 1))
                        .end(LocalDate.now())
                        .build()
                )
            )
            .build(),

        AttributeGroup.builder()
            .groupReference(URI.create("https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab"))
            .name("referenced testgroup")
            .id(referenceId)
            .includeReferenceOnly(true)
            .attributes(List.of(
                Attribute.builder().attributeRef("Observation.identifier").build(),
                Attribute.builder().attributeRef("Observation.status").build(),
                Attribute.builder().attributeRef("Observation.category").build(),
                Attribute.builder().attributeRef("Observation.code").build(),
                Attribute.builder().attributeRef("Observation.effective[x]").build()
            ))
            .filter(
                List.of(
                    Filter.builder()
                        .type("token")
                        .name("code")
                        .codes(List.of(
                            createTermCode()
                        ))
                        .build()
                )
            )
            .build()
        );
  }

  private Ccdl createValidCcdl(ValueFilterType valueFilterType, boolean withTimerestriction, boolean withAfterDate, boolean withBeforeDate) {
    var termCode = TermCode.builder()
        .code("LL2191-6")
        .system("http://loinc.org")
        .display("Geschlecht")
        .build();
    var inclusionCriterion = Criterion.builder()
        .context(termCode)
        .termCodes(List.of(termCode))
        .attributeFilters(List.of(
            createAttributeFilter(ValueFilterType.CONCEPT),
            createAttributeFilter(ValueFilterType.REFERENCE),
            createAttributeFilter(ValueFilterType.QUANTITY_COMPARATOR)
        ))
        .valueFilter(createValueFilter(valueFilterType))
        .timeRestriction(withTimerestriction ? createTimeRestriction(withAfterDate, withBeforeDate) : null)
        .build();
    return Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(inclusionCriterion)))
        .exclusionCriteria(null)
        .display("foo")
        .build();
  }

  private ValueFilter createValueFilter(ValueFilterType valueFilterType) {
    return switch (valueFilterType) {
      case CONCEPT -> ValueFilter.builder()
          .type(valueFilterType)
          .selectedConcepts(List.of(
              createTermCode()
          ))
          .build();
      case QUANTITY_COMPARATOR -> ValueFilter.builder()
          .type(valueFilterType)
          .comparator(Comparator.EQUAL)
          .value(42.0)
          .quantityUnit(Unit.builder()
              .code("a")
              .display("a")
              .build())
          .build();
      case QUANTITY_RANGE -> ValueFilter.builder()
          .type(valueFilterType)
          .minValue(18.0)
          .maxValue(42.0)
          .quantityUnit(Unit.builder()
              .code("a")
              .display("a")
              .build())
          .build();
      case REFERENCE -> ValueFilter.builder()
          .type(valueFilterType)
          .build();
    };
  }

  private AttributeFilter createAttributeFilter(ValueFilterType valueFilterType) {
    return switch (valueFilterType) {
      case CONCEPT -> AttributeFilter.builder()
          .type(valueFilterType)
          .attributeCode(createTermCode())
          .selectedConcepts(List.of(
              createTermCode()
          ))
          .build();
      case QUANTITY_COMPARATOR -> AttributeFilter.builder()
          .type(valueFilterType)
          .attributeCode(createTermCode())
          .comparator(Comparator.EQUAL)
          .value(42.0)
          .quantityUnit(Unit.builder()
              .code("a")
              .display("a")
              .build())
          .build();
      case QUANTITY_RANGE -> AttributeFilter.builder()
          .type(valueFilterType)
          .attributeCode(createTermCode())
          .minValue(18.0)
          .maxValue(42.0)
          .quantityUnit(Unit.builder()
              .code("a")
              .display("a")
              .build())
          .build();
      case REFERENCE -> AttributeFilter.builder()
          .type(valueFilterType)
          .attributeCode(createTermCode())
          .build();
    };
  }

  private DisplayEntry createDisplayEntry() {
    return DisplayEntry.builder()
        .original("original entry")
        .translations(List.of(
            LocalizedValue.builder().language("de-DE").value("deutscher Eintrag").build(),
            LocalizedValue.builder().language("en-US").value("English entry").build()
        ))
        .build();
  }

  private DseProfile createDseProfile() throws IOException {
    var dseProfile = new DseProfile();
    FileInputStream fis = new FileInputStream("src/test/resources/de/numcodex/feasibility_gui_backend/query/dataquery/dseProfileDiagnose.json");
    String dseProfileString = IOUtils.toString(fis, StandardCharsets.UTF_8);

    dseProfile.setId(1L);
    dseProfile.setEntry(dseProfileString);
    dseProfile.setUrl("https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose");
    return dseProfile;
  }

  private UiProfile createUiProfile() {
    var uiProfile = new UiProfile();
    uiProfile.setId(1L);
    uiProfile.setName("ui profile");
    uiProfile.setUiProfile("""
        {
            "attributeDefinitions": [],
            "name": "Diagnose",
            "timeRestrictionAllowed": true,
            "valueDefinition": null
        }
        """);
    return uiProfile;
  }

  private de.numcodex.feasibility_gui_backend.dse.api.DseProfile createDseProfileApi() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(createDseProfile().getEntry(), de.numcodex.feasibility_gui_backend.dse.api.DseProfile.class);
  }

  private de.numcodex.feasibility_gui_backend.terminology.api.UiProfile createUiProfileApi() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(createUiProfile().getUiProfile(), de.numcodex.feasibility_gui_backend.terminology.api.UiProfile.class);
  }

  private TimeRestriction createTimeRestriction(boolean includeAfterDate, boolean includeBeforeDate) {
    return TimeRestriction.builder()
        .afterDate(includeAfterDate ? "2025-04-16" : null)
        .beforeDate(includeBeforeDate ? "2025-04-17" : null)
        .build();
  }

  private TermCode createTermCode() {
    return TermCode.builder()
        .code("code")
        .system("system")
        .version("version")
        .display("display")
        .build();
  }
}
