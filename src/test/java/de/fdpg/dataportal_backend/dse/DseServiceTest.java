package de.fdpg.dataportal_backend.dse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import de.fdpg.dataportal_backend.common.api.DisplayEntry;
import de.fdpg.dataportal_backend.dse.api.*;
import de.fdpg.dataportal_backend.dse.persistence.DseProfile;
import de.fdpg.dataportal_backend.dse.persistence.DseProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class DseServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Mock
  private DseProfileRepository dseProfileRepository;
  private DseService dseService;

  private DseService createDseService() throws IOException {
    return new DseService("src/test/resources/ontology/dse/profile_tree.json", dseProfileRepository, objectMapper);
  }

  @BeforeEach
  public void setup() throws IOException {
    Mockito.reset(dseProfileRepository);
    dseService = createDseService();
  }

  @Test
  void testCreateProfileTreeInstance_throwsOnProfileTreeNotFound() {
    assertThrows(FileNotFoundException.class, ()
        -> new DseService("src/test/this/is/not/found.json", dseProfileRepository, objectMapper));
  }

  @Test
  void testGetProfileTree_succeeds() {

    var profileTree = assertDoesNotThrow(() -> dseService.getProfileTree());

    assertNotNull(profileTree);
    assertInstanceOf(DseProfileTreeNode.class, profileTree);
    assertEquals(profileTree.name(), "Root");
    assertEquals(profileTree.module(), "no-module");
    assertEquals(profileTree.url(), "no-url");
    assertEquals(profileTree.children().size(), 1);
    assertEquals(profileTree.children().get(0).id(), "de3323a2-7289-45e9-8a89-53c594f190e8");
  }

  @Test
  void testGetProfileTree_throwsOnObjectMapperError() {
    assertThrows(UnrecognizedPropertyException.class, ()
        -> new DseService("src/test/resources/ontology/dse/bogus_profile_tree.json", dseProfileRepository, objectMapper));
  }

  @Test
  void testGetProfileData_succeedsWithEmptyList() {
    var results = assertDoesNotThrow(() -> dseService.getProfileData(List.of()));

    assertNotNull(results);
    assertEquals(0, results.size());
  }

  @Test
  void testGetProfileData_succeedsWithoutErrors() throws JsonProcessingException {
    doReturn(Optional.of(createDummyDseProfile())).when(dseProfileRepository).findByUrl(any(String.class));

    var results = assertDoesNotThrow(() -> dseService.getProfileData(List.of("1")));

    assertNotNull(results);
    assertEquals(1, results.size());
  }

  @Test
  void testGetProfileData_succeedsWithErrorEntryOnCaughtException() throws JsonProcessingException {
    doThrow(DataIntegrityViolationException.class).when(dseProfileRepository).findByUrl(any(String.class));

    var results = assertDoesNotThrow(() -> dseService.getProfileData(List.of("1")));

    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals("TBD-00000", results.get(0).errorCode());
    assertEquals("profile not found", results.get(0).errorCause());
    assertEquals("1", results.get(0).url());
  }

  @Test
  void testGetProfileData_succeedsWithErrors() throws JsonProcessingException {
    doReturn(Optional.of(createDummyDseProfile())).when(dseProfileRepository).findByUrl("found");
    doReturn(Optional.empty()).when(dseProfileRepository).findByUrl("not-found");

    var results = assertDoesNotThrow(() -> dseService.getProfileData(List.of("found", "not-found")));

    assertNotNull(results);
    assertEquals(2, results.size());
    assertNull(results.get(0).errorCode());
    assertNotNull(results.get(1).errorCode());
  }

  @Test
  void testGetProfileData_throwsOnJsonProcessingException() {
    doReturn(Optional.of(createDummyDseProfileWithBogusEntry())).when(dseProfileRepository).findByUrl(any(String.class));
    assertThrows(RuntimeException.class, () -> dseService.getProfileData(List.of("1")));
  }

  private DseProfile createDummyDseProfile() throws JsonProcessingException {
    var dseProfile = new DseProfile();

    dseProfile.setId(1L);
    dseProfile.setUrl("http://example.com");
    dseProfile.setEntry(objectMapper.writeValueAsString(createDummyDseProfileEntry()));

    return dseProfile;
  }

  private DseProfile createDummyDseProfileWithBogusEntry() {
    var dseProfile = new DseProfile();

    dseProfile.setId(1L);
    dseProfile.setUrl("http://example.com");
    dseProfile.setEntry("something that can't be parsed as dse profile");

    return dseProfile;
  }

  private de.fdpg.dataportal_backend.dse.api.DseProfile createDummyDseProfileEntry() {

    return de.fdpg.dataportal_backend.dse.api.DseProfile.builder()
        .url("http://example.com")
        .display(createDummyDisplayEntry())
        .module(createDummyDisplayEntry())
        .fields(List.of(createDummyField()))
        .filters(List.of(createDummyFilter()))
        .references(List.of(createDummyReference()))
        .build();
  }

  private Filter createDummyFilter() {
    return Filter.builder()
        .name("filtername")
        .type("filtertype")
        .uiType("filterui")
        .valueSetUrls(List.of("http://example.com"))
        .build();
  }

  private Field createDummyField() {
    return Field.builder()
        .id("fieldid")
        .required(false)
        .recommended(true)
        .display(createDummyDisplayEntry())
        .description(createDummyDisplayEntry())
        .children(List.of())
        .build();
  }

  private Reference createDummyReference() {
    return Reference.builder()
        .id("reference-id")
        .display(createDummyDisplayEntry())
        .description(createDummyDisplayEntry())
        .type("reference")
        .recommended(true)
        .required(false)
        .children(List.of())
        .referencedProfiles(List.of(createDummyReferencedProfile()))
        .build();
  }

  private ReferencedProfile createDummyReferencedProfile() {
    return ReferencedProfile.builder()
        .display(createDummyDisplayEntry())
        .fields(createDummyFieldDisplayEntry())
        .url("http://some.url")
        .build();
  }

  private FieldDisplayEntry createDummyFieldDisplayEntry() {
    return FieldDisplayEntry.builder()
        .original(List.of("original-value"))
        .translations(List.of(LocalizedValueList.builder()
            .language("de-De")
            .value(List.of("value-value"))
            .build()))
        .build();
  }

  private DisplayEntry createDummyDisplayEntry() {
    return DisplayEntry.builder()
        .original("some-display")
        .translations(List.of(createDummyTranslation()))
        .build();
  }

  private LocalizedValue createDummyTranslation() {
    return LocalizedValue.builder()
        .language("en")
        .value("display value")
        .build();
  }
}
