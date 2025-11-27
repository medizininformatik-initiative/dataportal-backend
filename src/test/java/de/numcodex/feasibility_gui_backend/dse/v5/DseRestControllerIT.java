package de.numcodex.feasibility_gui_backend.dse.v5;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.dse.DseService;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import de.numcodex.feasibility_gui_backend.dse.api.DseProfile;
import de.numcodex.feasibility_gui_backend.dse.api.DseProfileTreeNode;
import de.numcodex.feasibility_gui_backend.dse.api.LocalizedValue;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingInterceptor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.net.URL;
import java.util.List;

import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("terminology")
@ExtendWith(SpringExtension.class)
@WebMvcTest(
    controllers = DseRestController.class
)
class DseRestControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper jsonUtil;

  @MockitoBean
  private DseService dseService;

  @MockitoBean
  private RateLimitingInterceptor rateLimitingInterceptor;

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  public void testGetProfileTree_succeedsWith200() throws Exception {
    doReturn(jsonUtil.readValue(new URL("file:src/test/resources/ontology/dse/profile_tree.json"), DseProfileTreeNode.class)).when(dseService).getProfileTree();

    mockMvc.perform(get(URI.create(PATH_API + PATH_DSE + "/profile-tree")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.name").value("Root"))
        .andExpect(jsonPath("$.children", hasSize(1)))
        .andExpect(jsonPath("$.children.[0].children", hasSize(5)));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  public void testGetProfileTree_failsOnFileNotFound() throws Exception {
    doReturn(null).when(dseService).getProfileTree();

    mockMvc.perform(get(URI.create(PATH_API + PATH_DSE + "/profile-tree")).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testGetProfileData_succeedsWith200OnFoundProfile() throws Exception {
    doReturn(List.of(createDummyDseProfileEntry())).when(dseService).getProfileData(anyList());

    mockMvc.perform(get(URI.create(PATH_API + PATH_DSE + "/profile-data")).param("ids", "https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab,foobar").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].display.original").value("some-display"));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testGetProfileData_succeedsWith200OnNoFoundProfile() throws Exception {
    doReturn(List.of()).when(dseService).getProfileData(anyList());

    mockMvc.perform(get(URI.create(PATH_API + PATH_DSE + "/profile-data")).param("ids", "https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab,foobar").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(0)));
  }

  private DseProfile createDummyDseProfileEntry() {

    return DseProfile.builder()
        .url("http://example.com")
        .display(createDummyDisplayEntry())
        .fields(List.of())
        .filters(List.of())
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