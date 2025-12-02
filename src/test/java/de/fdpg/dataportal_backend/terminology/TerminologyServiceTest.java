package de.fdpg.dataportal_backend.terminology;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fdpg.dataportal_backend.common.api.DisplayEntry;
import de.fdpg.dataportal_backend.terminology.api.CriteriaProfileData;
import de.fdpg.dataportal_backend.terminology.api.EsSearchResultEntry;
import de.fdpg.dataportal_backend.terminology.api.UiProfileEntry;
import de.fdpg.dataportal_backend.terminology.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@Tag("terminology")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TerminologyServiceTest {
  private static UUID VALID_NODE_ID_CAT1 = UUID.fromString("72ceaea9-c1ff-2e94-5fc0-7ba34feca654");

  private static UUID INVALID_NODE_ID = UUID.fromString("00000000-1111-2222-3333-444444444444");
  private final ObjectMapper jsonUtil = new ObjectMapper();
  @Mock
  private UiProfileRepository uiProfileRepository;
  @Mock
  private TermCodeRepository termCodeRepository;
  @Mock
  private Resource terminologySystemsResource;

  private TerminologyService createTerminologyService() throws IOException {
    return new TerminologyService("src/test/resources/ontology/terminology_systems.json", uiProfileRepository, termCodeRepository, jsonUtil);
  }

  @BeforeEach
  void setUp() {
    Mockito.reset(uiProfileRepository, termCodeRepository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "false"})
  void isExistingTermCode(boolean doesExist) throws IOException {
    var terminologyService = createTerminologyService();
    doReturn(doesExist).when(termCodeRepository).existsTermCode(any(String.class), any(String.class));
    doReturn(doesExist).when(termCodeRepository).existsTermCode(any(String.class), any(String.class), any(String.class));

    boolean termCodeResult = terminologyService.isExistingTermCode("some-system", "some-code");

    assertEquals(termCodeResult, doesExist);
  }

  @Test
  void testMin() {
    int expected = 3;
    int[] numbers = {33, 18, 3, 30, 4};

    int result = TerminologyService.min(numbers);

    assertEquals(expected, result);
  }

  @Test
  void testMin_emptyArray() {
    int expected = Integer.MAX_VALUE;
    int[] numbers = {};

    int result = TerminologyService.min(numbers);

    assertEquals(expected, result);
  }

  @Test
  void getCriteriaProfileData_emptyIdsResultInEmptyList() throws IOException {
    var terminologyService = createTerminologyService();

    var result = assertDoesNotThrow(() -> terminologyService.getCriteriaProfileData(List.of()));
    assertTrue(result.isEmpty());
  }

  @ParameterizedTest
  @CsvSource({
      "true, true, true",
      "true, true, false",
      "true, false, true",
      "true, false, false",
      "false, true, true",
      "false, true, false",
      "false, false, true",
      "false, false, false"
  })
  void getCriteriaProfileData(String noUiProfile, String noContext, String noTermcodes) throws IOException {
    var terminologyService = createTerminologyService();
    List<String> ids = List.of("123", "456", "789");
    boolean excludeUiProfile = Boolean.parseBoolean(noUiProfile);
    boolean excludeContext = Boolean.parseBoolean(noContext);
    boolean excludeTermcodes = Boolean.parseBoolean(noTermcodes);

    if (excludeUiProfile) {
      doReturn(Optional.empty()).when(uiProfileRepository).getUiProfileNameByContextualizedTermcodeHash(any(String.class));
    } else {
      doReturn(Optional.of("Patient")).when(uiProfileRepository).getUiProfileNameByContextualizedTermcodeHash(any(String.class));
    }
    if (excludeContext) {
      doReturn(Optional.empty()).when(termCodeRepository).findContextByContextualizedTermcodeHash(any(String.class));
    } else {
      doReturn(Optional.of(createContext())).when(termCodeRepository).findContextByContextualizedTermcodeHash(any(String.class));
    }
    if (excludeTermcodes) {
      doReturn(Optional.empty()).when(termCodeRepository).findTermCodeByContextualizedTermcodeHash(any(String.class));
    } else {
      doReturn(Optional.of(createTermCode())).when(termCodeRepository).findTermCodeByContextualizedTermcodeHash(any(String.class));
    }

    var result = assertDoesNotThrow(() -> terminologyService.getCriteriaProfileData(ids));

    assertThat(result.size()).isEqualTo(ids.size());

    for (int i = 0; i < result.size(); i++) {
      CriteriaProfileData cpd = result.get(i);
      assertThat(cpd.id()).isEqualTo(ids.get(i));
      assertThat(cpd.context() == null).isEqualTo(excludeContext);
      assertThat(cpd.termCodes().isEmpty()).isEqualTo(excludeTermcodes);
      assertThat(cpd.uiProfileId().equalsIgnoreCase("undefined")).isEqualTo(excludeUiProfile);
    }
  }

  @Test
  void getUiProfiles_succeeds() throws IOException {
    var terminologyService = createTerminologyService();
    doReturn(List.of(createUiProfile())).when(uiProfileRepository).findAll();

    var result = assertDoesNotThrow(terminologyService::getUiProfiles);

    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0)).isInstanceOf(UiProfileEntry.class);
  }

  @Test
  void getUiProfiles_throwsOnInvalidProfile() throws IOException {
    var terminologyService = createTerminologyService();
    doReturn(List.of(createBogousUiProfile())).when(uiProfileRepository).findAll();

    assertThrows(RuntimeException.class, () -> terminologyService.getUiProfiles());
  }

  @Test
  void getTerminologySystems_succeeds() throws IOException, NoSuchFieldException, IllegalAccessException {
    var terminologyService = createTerminologyService();

    var terminologySystems = assertDoesNotThrow(terminologyService::getTerminologySystems);

    assertFalse(terminologySystems.isEmpty());
    assertThat(terminologySystems.size()).isEqualTo(4);
    assertThat(terminologySystems.get(0).name()).isEqualTo("loinc");
    assertThat(terminologySystems.get(2).url()).isEqualTo("http://fhir.de/CodeSystem/bfarm/ops");
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "false"})
  void testAddDisplay(boolean matchingIds) throws IOException {
    var terminologyService = createTerminologyService();
    de.fdpg.dataportal_backend.common.api.TermCode tc = de.fdpg.dataportal_backend.common.api.TermCode.builder()
        .code("code")
        .system("system")
        .display("display")
        .version("version")
        .build();

    CriteriaProfileData cpd = CriteriaProfileData.builder()
        .id("id")
        .context(de.fdpg.dataportal_backend.common.api.TermCode.builder()
            .code("code")
            .system("system")
            .display("display")
            .version("version")
            .build())
        .termCodes(List.of(tc))
        .uiProfileId("Patient")
        .build();

    EsSearchResultEntry esSearchResultEntry = EsSearchResultEntry.builder()
        .id(matchingIds ? "id" : "differentId")
        .display(DisplayEntry.builder().build())
        .availability(100)
        .context("context")
        .terminology("terminology")
        .termcode("termcode")
        .kdsModule("kdsModule")
        .selectable(true)
        .build();

    assertNull(cpd.display());

    var criteriaProfileDataWithDisplayData = terminologyService.addDisplayDataToCriteriaProfileData(List.of(cpd), List.of(esSearchResultEntry));
    assertNotNull(criteriaProfileDataWithDisplayData);

    if (matchingIds) {
      assertNotNull(criteriaProfileDataWithDisplayData.get(0).display());
    } else {
      assertNull(criteriaProfileDataWithDisplayData.get(0).display());
    }
  }

  private UiProfile createUiProfile() throws JsonProcessingException {
    var uiProfile = new UiProfile();
    uiProfile.setId(1L);
    uiProfile.setName("example");
    uiProfile.setUiProfile(jsonUtil.writeValueAsString(
        de.fdpg.dataportal_backend.terminology.api.UiProfile.builder()
            .name("ExampleProfile")
            .timeRestrictionAllowed(true)
            .build())
    );
    return uiProfile;
  }

  private UiProfile createBogousUiProfile() throws JsonProcessingException {
    var uiProfile = new UiProfile();
    uiProfile.setId(1L);
    uiProfile.setName("example");
    uiProfile.setUiProfile("this is not a valid profile entry");
    return uiProfile;
  }

  private TermCode createTermCode() {
    TermCode termCode = new TermCode();
    termCode.setId(1L);
    termCode.setCode("LL2191-6");
    termCode.setSystem("http://loinc.org");
    termCode.setVersion("1.0.0");
    termCode.setDisplay("Geschlecht");
    return termCode;
  }

  private Context createContext() {
    Context context = new Context();
    context.setId(1L);
    context.setCode("LL2191-6");
    context.setSystem("http://loinc.org");
    context.setVersion("1.0.0");
    context.setDisplay("Geschlecht");
    return context;
  }
}
