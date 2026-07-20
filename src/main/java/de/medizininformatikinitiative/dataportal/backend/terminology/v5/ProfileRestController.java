package de.medizininformatikinitiative.dataportal.backend.terminology.v5;

import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.ProfileSearchResult;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.ProfileService;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.TermFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v5/profile")
@ConditionalOnExpression("${app.elastic.enabled}")
@CrossOrigin
public class ProfileRestController {

  private final ProfileService profileService;

  @Autowired
  public ProfileRestController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @GetMapping(value = "/entry/search", produces = MediaType.APPLICATION_JSON_VALUE)
  public ProfileSearchResult searchProfiles(@RequestParam(value = "searchterm", required = false, defaultValue = "") String keyword,
                                            @RequestParam(value = "modules", required = false) List<String> modules,
                                            @RequestParam(value = "categories", required = false) List<String> categories,
                                            @RequestParam(value = "page-size", required = false, defaultValue = "20") int pageSize,
                                            @RequestParam(value = "page", required = false, defaultValue = "0") int page) {

    return profileService.performProfileSearchWithRepoAndPaging(keyword, modules, categories, pageSize, page);
  }

  @GetMapping(value = "/entry/{id}/list-details", produces = MediaType.APPLICATION_JSON_VALUE)
  public ProfileEntry getProfileListDetailsById(@PathVariable("id") String id) {
    return profileService.getProfileListDetailsById(id);
  }

  @GetMapping(value = "/search/filter", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<TermFilter> getFilter() {
    return profileService.getAvailableFilters();
  }
}
