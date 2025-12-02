package de.fdpg.dataportal_backend.dse.v5;

import de.fdpg.dataportal_backend.dse.DseService;
import de.fdpg.dataportal_backend.dse.api.DseProfile;
import de.fdpg.dataportal_backend.dse.api.DseProfileTreeNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static de.fdpg.dataportal_backend.config.WebSecurityConfig.PATH_API;
import static de.fdpg.dataportal_backend.config.WebSecurityConfig.PATH_DSE;

@RequestMapping(PATH_API + PATH_DSE)
@RestController
@CrossOrigin
public class DseRestController {

  private final DseService dseService;

  public DseRestController(DseService dseService) {
    this.dseService = dseService;
  }

  @GetMapping(value = "profile-tree", produces = MediaType.APPLICATION_JSON_VALUE)
  public DseProfileTreeNode getProfileTree() {
    DseProfileTreeNode profileTree = dseService.getProfileTree();
    if (profileTree == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DSE profile tree not found");
    } else {
      return profileTree;
    }
  }

  @GetMapping(value = "profile-data", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<DseProfile> getProfileData(@RequestParam List<String> ids) {
    return dseService.getProfileData(ids);
  }
}
