package de.medizininformatikinitiative.dataportal.backend.validation.v5;

import com.fasterxml.jackson.databind.JsonNode;
import de.medizininformatikinitiative.dataportal.backend.validation.CrtdlUpgradeException;
import de.medizininformatikinitiative.dataportal.backend.validation.UpgradeService;
import de.medizininformatikinitiative.dataportal.backend.validation.ValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

  @PostMapping(PATH_CCDL)
  public ResponseEntity<?> validateCcdl(@RequestBody JsonNode queryNode) {
    return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
  }

  @PostMapping(path = PATH_CRTDL)
  public ResponseEntity<Object> validateCrtdl(@RequestBody JsonNode crtdlNode) {
    return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
  }

  @PostMapping("/dataquery")
  public ResponseEntity<Object> upgradeDataquery(
      @RequestBody JsonNode dataqueryJsonNode) {

    // Validate Schema
    var schemaValidationErrors = validationService.validateDataquerySchema(dataqueryJsonNode);
    if (!schemaValidationErrors.isEmpty()) {
      return new ResponseEntity<>(schemaValidationErrors, HttpStatus.BAD_REQUEST);
    }

    // Validate Content
    var dataquery = validationService.dataqueryFromJsonNode(dataqueryJsonNode);
    var upgradedCrtdl = upgradeService.upgrade(dataquery.content());
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
