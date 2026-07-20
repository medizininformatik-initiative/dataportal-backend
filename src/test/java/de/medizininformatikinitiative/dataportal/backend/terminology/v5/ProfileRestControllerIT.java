package de.medizininformatikinitiative.dataportal.backend.terminology.v5;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.dse.api.LocalizedValue;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.RateLimitingInterceptor;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileRelativeEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileSearchEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileSearchResult;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.ProfileService;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.TermFilter;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.TermFilterValue;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.ProfileNotFoundException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.PATH_API;
import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.PATH_PROFILE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("terminology")
@Tag("elasticsearch")
@ExtendWith(SpringExtension.class)
@Import(RateLimitingServiceSpringConfig.class)
@WebMvcTest(
    controllers = ProfileRestController.class
)
class ProfileRestControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProfileService profileService;

  @MockitoBean
  private RateLimitingInterceptor rateLimitingInterceptor;

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testSearchProfiles_succeedsWith200() throws Exception {
    var dummySearchResult = createDummyProfileSearchResult();
    doReturn(dummySearchResult).when(profileService)
        .performProfileSearchWithRepoAndPaging(any(String.class), isNull(), isNull(), anyInt(), anyInt());

    mockMvc.perform(get(URI.create(PATH_API + PATH_PROFILE + "/entry/search"))
            .param("searchterm", "foo")
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalHits").value(dummySearchResult.getTotalHits()))
        .andExpect(jsonPath("$.results.length()").value(dummySearchResult.getResults().size()))
        .andExpect(jsonPath("$.results[0].id").value(dummySearchResult.getResults().get(0).id()))
        .andExpect(jsonPath("$.results[0].name").value(dummySearchResult.getResults().get(0).name()))
        .andExpect(jsonPath("$.results[0].display.original").value(dummySearchResult.getResults().get(0).display().original()))
        .andExpect(jsonPath("$.results[0].description").doesNotExist())
        .andExpect(jsonPath("$.results[0].parents").doesNotExist())
        .andExpect(jsonPath("$.results[0].children").doesNotExist());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testSearchProfiles_succeedsWithModuleAndCategoriesFilter() throws Exception {
    var dummySearchResult = createDummyProfileSearchResult();
    doReturn(dummySearchResult).when(profileService)
        .performProfileSearchWithRepoAndPaging(any(String.class), anyList(), anyList(), anyInt(), anyInt());

    mockMvc.perform(get(URI.create(PATH_API + PATH_PROFILE + "/entry/search"))
            .param("searchterm", "foo")
            .param("modules", "Diagnose")
            .param("categories", "category-a")
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalHits").value(dummySearchResult.getTotalHits()));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testGetProfileListDetailsById_succeedsWith200() throws Exception {
    var id = UUID.randomUUID().toString();
    var dummyProfileEntry = createDummyProfileEntry(id);
    doReturn(dummyProfileEntry).when(profileService).getProfileListDetailsById(id);

    mockMvc.perform(get(URI.create(PATH_API + PATH_PROFILE + "/entry/" + id + "/list-details")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(dummyProfileEntry.id()))
        .andExpect(jsonPath("$.name").value(dummyProfileEntry.name()))
        .andExpect(jsonPath("$.description.original").value(dummyProfileEntry.description().original()))
        .andExpect(jsonPath("$.fields.length()").value(dummyProfileEntry.fields().size()))
        .andExpect(jsonPath("$.fields[0].original").value(dummyProfileEntry.fields().get(0).original()))
        .andExpect(jsonPath("$.parents.length()").value(dummyProfileEntry.parents().size()))
        .andExpect(jsonPath("$.parents[0].id").value(dummyProfileEntry.parents().get(0).id()))
        .andExpect(jsonPath("$.parents[0].url").value(dummyProfileEntry.parents().get(0).url()))
        .andExpect(jsonPath("$.parents[0].display.original").value(dummyProfileEntry.parents().get(0).display().original()))
        .andExpect(jsonPath("$.children.length()").value(dummyProfileEntry.children().size()))
        .andExpect(jsonPath("$.children[0].url").value(dummyProfileEntry.children().get(0).url()));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testGetProfileListDetailsById_failsWith404OnNotFound() throws Exception {
    doThrow(ProfileNotFoundException.class).when(profileService).getProfileListDetailsById(any(String.class));

    mockMvc.perform(get(URI.create(PATH_API + PATH_PROFILE + "/entry/does-not-exist/list-details")).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testGetFilter_succeedsWith200() throws Exception {
    var dummyFilters = createDummyFilters();
    doReturn(dummyFilters).when(profileService).getAvailableFilters();

    mockMvc.perform(get(URI.create(PATH_API + PATH_PROFILE + "/search/filter")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(dummyFilters.size()))
        .andExpect(jsonPath("$[0].name").value("module"))
        .andExpect(jsonPath("$[0].values[0].label").value("Diagnose"))
        .andExpect(jsonPath("$[0].values[0].count").value(2))
        .andExpect(jsonPath("$[1].name").value("category"))
        .andExpect(jsonPath("$[1].values[0].label").value("element"));
  }

  private List<TermFilter> createDummyFilters() {
    return List.of(
        TermFilter.builder()
            .name("module")
            .type("selectable-concept")
            .values(List.of(TermFilterValue.builder().label("Diagnose").count(2).build()))
            .build(),
        TermFilter.builder()
            .name("category")
            .type("selectable-concept")
            .values(List.of(TermFilterValue.builder().label("element").count(1).build()))
            .build()
    );
  }

  private ProfileSearchResult createDummyProfileSearchResult() {
    return ProfileSearchResult.builder()
        .totalHits(1)
        .results(List.of(createDummyProfileSearchEntry(UUID.randomUUID().toString())))
        .build();
  }

  private ProfileSearchEntry createDummyProfileSearchEntry(String id) {
    return ProfileSearchEntry.builder()
        .id(id)
        .name("some-profile")
        .display(createDummyDisplayEntry())
        .selectable(true)
        .url("https://example.org/some-profile")
        .module(createDummyDisplayEntry())
        .categories(List.of(createDummyDisplayEntry()))
        .availability(1)
        .build();
  }

  private ProfileEntry createDummyProfileEntry(String id) {
    return ProfileEntry.builder()
        .id(id)
        .name("some-profile")
        .display(createDummyDisplayEntry())
        .description(createDummyDisplayEntry())
        .selectable(true)
        .url("https://example.org/some-profile")
        .module(createDummyDisplayEntry())
        .categories(List.of(createDummyDisplayEntry()))
        .availability(1)
        .fields(List.of(createDummyDisplayEntry()))
        .parents(List.of(createDummyProfileRelativeEntry()))
        .children(List.of(createDummyProfileRelativeEntry(), createDummyProfileRelativeEntry()))
        .build();
  }

  private ProfileRelativeEntry createDummyProfileRelativeEntry() {
    return ProfileRelativeEntry.builder()
        .id(UUID.randomUUID().toString())
        .display(createDummyDisplayEntry())
        .url("https://example.org/some-relative")
        .build();
  }

  private DisplayEntry createDummyDisplayEntry() {
    return DisplayEntry.builder()
        .original("Some Name")
        .translations(List.of(
            LocalizedValue.builder()
                .value("Some German Name")
                .language("de-DE")
                .build(),
            LocalizedValue.builder()
                .value("Some English Name")
                .language("en-US")
                .build()
        ))
        .build();
  }
}
