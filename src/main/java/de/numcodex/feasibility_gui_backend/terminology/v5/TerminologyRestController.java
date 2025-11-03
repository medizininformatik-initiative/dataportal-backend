package de.numcodex.feasibility_gui_backend.terminology.v5;


import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import de.numcodex.feasibility_gui_backend.terminology.api.*;
import de.numcodex.feasibility_gui_backend.terminology.es.TerminologyEsService;
import de.numcodex.feasibility_gui_backend.terminology.es.model.TermFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 Rest interface to get the terminology definitions from the UI backend which itself request the
 terminology information from the ui terminology service
 */


@RequestMapping("api/v5/terminology")
@RestController
@CrossOrigin
@ConditionalOnExpression("${app.elastic.enabled}")
public class TerminologyRestController {

    private final TerminologyService terminologyService;

    private TerminologyEsService terminologyEsService;

    @Autowired
    public TerminologyRestController(TerminologyService terminologyService, TerminologyEsService terminologyEsService) {
        this.terminologyService = terminologyService;
        this.terminologyEsService = terminologyEsService;
    }

    @GetMapping("criteria-profile-data")
    public List<CriteriaProfileData> getCriteriaProfileData(@RequestParam List<String> ids) {
        var criteriaProfileData = terminologyService.getCriteriaProfileData(ids);
        var displayData = terminologyEsService.getSearchResultEntriesByHash(ids);
        return terminologyService.addDisplayDataToCriteriaProfileData(criteriaProfileData, displayData);
    }

    @GetMapping("ui-profile")
    public List<UiProfileEntry> getUiProfiles() {
      return terminologyService.getUiProfiles();
    }

    @GetMapping(value = "systems", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TerminologySystemEntry> getTerminologySystems() {
        return terminologyService.getTerminologySystems();
    }

    @GetMapping("search/filter")
    public List<TermFilter> getAvailableFilters() {
        return terminologyEsService.getAvailableFilters();
    }

    @GetMapping("entry/search")
    public EsSearchResult searchOntologyItemsCriteriaQuery(@RequestParam("searchterm") String keyword,
                                                           @RequestParam(value = "criteria-sets", required = false) List<String> criteriaSets,
                                                           @RequestParam(value = "contexts", required = false) List<String> contexts,
                                                           @RequestParam(value = "kds-modules", required = false) List<String> kdsModules,
                                                           @RequestParam(value = "terminologies", required = false) List<String> terminologies,
                                                           @RequestParam(value = "availability", required = false, defaultValue = "false") boolean availability,
                                                           @RequestParam(value = "page-size", required = false, defaultValue = "20") int pageSize,
                                                           @RequestParam(value = "page", required = false, defaultValue = "0") int page) {


        return terminologyEsService
            .performOntologySearchWithPaging(keyword, criteriaSets, contexts, kdsModules, terminologies, availability, pageSize, page);
    }

    @GetMapping("entry/{hash}/relations")
    public RelationEntry getOntologyItemRelationsByHash(@PathVariable("hash") String hash) {
        return terminologyEsService.getRelationEntryByHash(hash);
    }

    @GetMapping("entry/{hash}")
    public EsSearchResultEntry getOntologyItemByHash(@PathVariable("hash") String hash) {
        return terminologyEsService.getSearchResultEntryByHash(hash);
    }
}
