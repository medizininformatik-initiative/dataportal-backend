package de.medizininformatikinitiative.dataportal.backend.validation.v5;

import tools.jackson.databind.JsonNode;
import de.medizininformatikinitiative.dataportal.backend.validation.UpgradeService;
import de.medizininformatikinitiative.dataportal.backend.validation.ValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.*;

@RequestMapping(PATH_API + PATH_UPGRADE)
@RestController
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = "Location")
public class UpgradeRestController {

  private final ValidationService validationService;
  private final UpgradeService upgradeService;

  public UpgradeRestController(ValidationService validationService, UpgradeService upgradeService) {
    this.validationService = validationService;
    this.upgradeService = upgradeService;
  }

  @PostMapping(path = PATH_CRTDL)
  public ResponseEntity<Object> upgradeCrtdl(@RequestBody JsonNode crtdlNode) {
    // Validate Schema
    var schemaValidationErrors = validationService.validateCrtdlSchema(crtdlNode);
    if (!schemaValidationErrors.isEmpty()) {
      return new ResponseEntity<>(schemaValidationErrors, HttpStatus.BAD_REQUEST);
    }

    // Validate Content
    var crtdl = validationService.crtdlFromJsonNode(crtdlNode);
    var upgradedCrtdl = upgradeService.upgrade(crtdl);
    return new ResponseEntity<>(upgradedCrtdl, HttpStatus.OK);
  }
}
