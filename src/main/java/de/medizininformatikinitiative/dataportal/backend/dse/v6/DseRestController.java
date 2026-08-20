package de.medizininformatikinitiative.dataportal.backend.dse.v6;

import de.medizininformatikinitiative.dataportal.backend.dse.DseService;
import de.medizininformatikinitiative.dataportal.backend.dse.api.DseProfile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.PATH_API;
import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.PATH_DSE;

@RequestMapping(PATH_API + PATH_DSE)
@RestController
@CrossOrigin
public class DseRestController {

  private final DseService dseService;

  public DseRestController(DseService dseService) {
    this.dseService = dseService;
  }

  @GetMapping(value = "profile-data", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<DseProfile> getProfileData(@RequestParam List<String> ids) {
    return dseService.getProfileData(ids);
  }
}
