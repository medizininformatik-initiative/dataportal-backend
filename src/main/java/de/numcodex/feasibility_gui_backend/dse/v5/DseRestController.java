package de.numcodex.feasibility_gui_backend.dse.v5;

import de.numcodex.feasibility_gui_backend.dse.DseService;
import de.numcodex.feasibility_gui_backend.dse.api.DseProfile;
import de.numcodex.feasibility_gui_backend.dse.api.DseProfileTreeNode;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_API;
import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_DSE;

@Tag(name = "DSE", description = "Data Selection and Extraction")
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
