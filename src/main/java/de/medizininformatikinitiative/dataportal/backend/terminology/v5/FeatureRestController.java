package de.medizininformatikinitiative.dataportal.backend.terminology.v5;

import de.medizininformatikinitiative.dataportal.backend.terminology.api.FeatureEntry;
import de.medizininformatikinitiative.dataportal.backend.terminology.api.FeatureSearchResult;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.FeatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v5/feature")
@ConditionalOnExpression("${app.elastic.enabled}")
@CrossOrigin
public class FeatureRestController {

  private final FeatureService featureService;

  @Autowired
  public FeatureRestController(FeatureService featureService) {
    this.featureService = featureService;
  }

  @GetMapping(value = "/entry/search", produces = MediaType.APPLICATION_JSON_VALUE)
  public FeatureSearchResult searchFeatures(@RequestParam(value = "searchterm", required = false, defaultValue = "") String keyword,
                                            @RequestParam(value = "modules", required = false) List<String> modules,
                                            @RequestParam(value = "categories", required = false) List<String> categories,
                                            @RequestParam(value = "page-size", required = false, defaultValue = "20") int pageSize,
                                            @RequestParam(value = "page", required = false, defaultValue = "0") int page) {

    return featureService.performFeatureSearchWithRepoAndPaging(keyword, modules, categories, pageSize, page);
  }

  @GetMapping(value = "/entry/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public FeatureEntry getFeatureById(@PathVariable("id") String id) {
    return featureService.getSearchResultEntryById(id);
  }
}
