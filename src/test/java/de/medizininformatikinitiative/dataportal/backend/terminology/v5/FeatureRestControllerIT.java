package de.medizininformatikinitiative.dataportal.backend.terminology.v5;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import de.medizininformatikinitiative.dataportal.backend.dse.api.LocalizedValue;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.RateLimitingInterceptor;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.FeatureEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.FeatureRelativeEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.FeatureSearchEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.FeatureSearchResult;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.FeatureService;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.repository.FeatureNotFoundException;
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
import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.PATH_FEATURE;
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
    controllers = FeatureRestController.class
)
class FeatureRestControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private FeatureService featureService;

  @MockitoBean
  private RateLimitingInterceptor rateLimitingInterceptor;

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testSearchFeatures_succeedsWith200() throws Exception {
    var dummySearchResult = createDummyFeatureSearchResult();
    doReturn(dummySearchResult).when(featureService)
        .performFeatureSearchWithRepoAndPaging(any(String.class), isNull(), isNull(), anyInt(), anyInt());

    mockMvc.perform(get(URI.create(PATH_API + PATH_FEATURE + "/entry/search"))
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
  void testSearchFeatures_succeedsWithModuleAndCategoriesFilter() throws Exception {
    var dummySearchResult = createDummyFeatureSearchResult();
    doReturn(dummySearchResult).when(featureService)
        .performFeatureSearchWithRepoAndPaging(any(String.class), anyList(), anyList(), anyInt(), anyInt());

    mockMvc.perform(get(URI.create(PATH_API + PATH_FEATURE + "/entry/search"))
            .param("searchterm", "foo")
            .param("modules", "Diagnose")
            .param("categories", "category-a")
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalHits").value(dummySearchResult.getTotalHits()));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testGetFeatureListDetailsById_succeedsWith200() throws Exception {
    var id = UUID.randomUUID().toString();
    var dummyFeatureEntry = createDummyFeatureEntry(id);
    doReturn(dummyFeatureEntry).when(featureService).getFeatureListDetailsById(id);

    mockMvc.perform(get(URI.create(PATH_API + PATH_FEATURE + "/entry/" + id + "/list-details")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(dummyFeatureEntry.id()))
        .andExpect(jsonPath("$.name").value(dummyFeatureEntry.name()))
        .andExpect(jsonPath("$.description.original").value(dummyFeatureEntry.description().original()))
        .andExpect(jsonPath("$.parents.length()").value(dummyFeatureEntry.parents().size()))
        .andExpect(jsonPath("$.parents[0].id").value(dummyFeatureEntry.parents().get(0).id()))
        .andExpect(jsonPath("$.children.length()").value(dummyFeatureEntry.children().size()));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testGetFeatureListDetailsById_failsWith404OnNotFound() throws Exception {
    doThrow(FeatureNotFoundException.class).when(featureService).getFeatureListDetailsById(any(String.class));

    mockMvc.perform(get(URI.create(PATH_API + PATH_FEATURE + "/entry/does-not-exist/list-details")).with(csrf()))
        .andExpect(status().isNotFound());
  }

  private FeatureSearchResult createDummyFeatureSearchResult() {
    return FeatureSearchResult.builder()
        .totalHits(1)
        .results(List.of(createDummyFeatureSearchEntry(UUID.randomUUID().toString())))
        .build();
  }

  private FeatureSearchEntry createDummyFeatureSearchEntry(String id) {
    return FeatureSearchEntry.builder()
        .id(id)
        .name("some-feature")
        .display(createDummyDisplayEntry())
        .selectable(true)
        .url("https://example.org/some-feature")
        .module(createDummyDisplayEntry())
        .categories(List.of("category-a"))
        .availability(1)
        .build();
  }

  private FeatureEntry createDummyFeatureEntry(String id) {
    return FeatureEntry.builder()
        .id(id)
        .name("some-feature")
        .display(createDummyDisplayEntry())
        .description(createDummyDisplayEntry())
        .selectable(true)
        .url("https://example.org/some-feature")
        .module(createDummyDisplayEntry())
        .categories(List.of("category-a"))
        .availability(1)
        .parents(List.of(createDummyFeatureRelativeEntry()))
        .children(List.of(createDummyFeatureRelativeEntry(), createDummyFeatureRelativeEntry()))
        .build();
  }

  private FeatureRelativeEntry createDummyFeatureRelativeEntry() {
    return FeatureRelativeEntry.builder()
        .id(UUID.randomUUID().toString())
        .display(createDummyDisplayEntry())
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
