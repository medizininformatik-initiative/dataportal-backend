package de.medizininformatikinitiative.dataportal.backend.query.v6;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import de.medizininformatikinitiative.dataportal.backend.query.api.Crtdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.CrtdlSectionInfo;
import de.medizininformatikinitiative.dataportal.backend.query.api.Dataquery;
import de.medizininformatikinitiative.dataportal.backend.query.dataquery.DataqueryCsvExportException;
import de.medizininformatikinitiative.dataportal.backend.query.dataquery.DataqueryException;
import de.medizininformatikinitiative.dataportal.backend.query.dataquery.DataqueryHandler;
import de.medizininformatikinitiative.dataportal.backend.query.dataquery.DataqueryStorageFullException;
import de.medizininformatikinitiative.dataportal.backend.validation.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;

import static de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.*;

/*
Rest Interface for the UI to send and receive dataqueries from the backend.
*/
@RequestMapping(PATH_API + PATH_QUERY + PATH_DATA)
@RestController("DataqueryHandlerRestController-v6")
@Slf4j
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = "Location")
public class DataqueryHandlerRestController {
  private final static String API_VERSION = "v6";
  private final DataqueryHandler dataqueryHandler;
  private final ValidationService validationService;

  public DataqueryHandlerRestController(DataqueryHandler dataqueryHandler,
                                        ValidationService validationService) {
    this.dataqueryHandler = dataqueryHandler;
    this.validationService = validationService;
  }

  @PostMapping(path = "")
  public ResponseEntity<Object> storeDataquery(@RequestBody Dataquery dataquery,
                                               Principal principal) {

    Long dataqueryId;
    try {
      dataqueryId = dataqueryHandler.storeDataquery(dataquery, principal.getName());
    } catch (DataqueryException e) {
      log.error("Error while storing dataquery", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataqueryStorageFullException e) {
      return new ResponseEntity<>("storage exceeded", HttpStatus.FORBIDDEN);
    }

    var dataquerySlots = dataqueryHandler.getDataquerySlotsJson(principal.getName());
    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(dataqueryId)
        .toUri();

    return ResponseEntity
        .created(location)
        .body(dataquerySlots);
  }

  @GetMapping(path = "/{dataqueryId}")
  public ResponseEntity<Object> getDataquery(@PathVariable(value = "dataqueryId") Long dataqueryId,
                                             @RequestParam(value = "skip-validation", required = false, defaultValue = "false") boolean skipValidation,
                                             Authentication authentication) {

    try {
      var dataquery = dataqueryHandler.getDataqueryById(dataqueryId, authentication);
      var dataqueryWithInvalidCriteria = Dataquery.builder()
          .id(dataquery.id())
          .content(dataquery.content())
          .label(dataquery.label())
          .comment(dataquery.comment())
          .lastModified(dataquery.lastModified())
          .createdBy(dataquery.createdBy())
          .resultSize(dataquery.resultSize())
          .ccdl(ccdlSectionInfo(dataquery.content(), skipValidation))
          .dataExtraction(dataExtractionSectionInfo(dataquery.content(), skipValidation))
          .build();
      return new ResponseEntity<>(dataqueryWithInvalidCriteria, HttpStatus.OK);
    } catch (JacksonException e) {
      log.error("Could not parse content of dataquery with id {}", dataqueryId, e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataqueryException e) {
      log.debug("Dataquery with id {} not found or not accessible", dataqueryId, e);
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping(path = "/{dataqueryId}" + PATH_CRTDL)
  public ResponseEntity<Object> getDataqueryCrtdl(@PathVariable(value = "dataqueryId") Long dataqueryId,
                                                  Authentication authentication) {

    try {
      var dataquery = dataqueryHandler.getDataqueryById(dataqueryId, authentication);
      return new ResponseEntity<>(dataquery.content(), HttpStatus.OK);
    } catch (JacksonException e) {
      log.error("Could not parse content of dataquery with id {}", dataqueryId, e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataqueryException e) {
      log.debug("Dataquery with id {} not found or not accessible", dataqueryId, e);
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping(path = "/{dataqueryId}" + PATH_CRTDL, produces = "application/zip")
  public ResponseEntity<Object> getDataqueryCrtdlCsv(@PathVariable(value = "dataqueryId") Long dataqueryId,
                                                     Authentication authentication) {
    try {
      var dataquery = dataqueryHandler.getDataqueryById(dataqueryId, authentication);
      var zipByteArrayOutputStream = dataqueryHandler.createCsvExportZipfile(dataquery);
      HttpHeaders headers = new HttpHeaders();
      String headerValue = "attachment; filename=" + dataquery.label().toUpperCase() + "_dataquery.zip";
      headers.add(HttpHeaders.CONTENT_DISPOSITION, headerValue);
      headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
      return new ResponseEntity<>(zipByteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);
    } catch (IOException | JacksonException e) {
      log.error("Could not create csv export for dataquery with id {}", dataqueryId, e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataqueryException e) {
      log.debug("Dataquery with id {} not found, not accessible or without ccdl", dataqueryId, e);
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (DataqueryCsvExportException e) {
      log.warn("Could not convert dataquery with id {} to csv", dataqueryId, e);
      return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }

  @PostMapping(path = "/convert" + PATH_CRTDL)
  public ResponseEntity<Object> convertCrtdlToCsv(@RequestBody JsonNode crtdlNode,
                                                  Authentication authentication) {
    var validationErrors = validationService.validateCrtdlSchema(crtdlNode);
    if (!validationErrors.isEmpty()) {
      return new ResponseEntity<>(validationErrors, HttpStatus.BAD_REQUEST);
    }

    var crtdl = validationService.crtdlFromJsonNode(crtdlNode);
    // the csv converter currently works on a dataquery object but just uses the crtdl part of it. So just create a dummy for that
    var dataquery = Dataquery.builder()
        .createdBy(authentication.getName())
        .id(-1L)
        .content(crtdl)
        .label("")
        .build();
    try {
      var zipByteArrayOutputStream = dataqueryHandler.createCsvExportZipfile(dataquery);
      HttpHeaders headers = new HttpHeaders();
      String headerValue = "attachment; filename=untitled.zip";
      headers.add(HttpHeaders.CONTENT_DISPOSITION, headerValue);
      headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
      return new ResponseEntity<>(zipByteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);
    } catch (IOException | JacksonException e) {
      log.error("Could not create csv export for submitted crtdl", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataqueryException e) {
      log.debug("Submitted crtdl does not contain a ccdl", e);
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (DataqueryCsvExportException e) {
      log.warn("Could not convert submitted crtdl to csv", e);
      return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }

  @GetMapping(path = "")
  public ResponseEntity<Object> getDataqueries(
      @RequestParam(value = "skip-validation", required = false, defaultValue = "false") boolean skipValidation,
      @RequestParam(value = "include-temporary", required = false, defaultValue = "false") boolean includeTemporary,
      Principal principal) {

    try {
      var dataqueries = dataqueryHandler.getDataqueriesByAuthor(principal.getName(), includeTemporary);
      var ret = new ArrayList<Dataquery>();
      dataqueries.forEach(dq -> {
        ret.add(
            Dataquery.builder()
                .id(dq.id())
                .label(dq.label())
                .comment(dq.comment())
                .lastModified(dq.lastModified())
                .resultSize(dq.resultSize())
                .ccdl(ccdlSectionInfo(dq.content(), skipValidation))
                .dataExtraction(dataExtractionSectionInfo(dq.content(), skipValidation))
                .expiresAt(dq.expiresAt())
                .build()
        );
      });
      return new ResponseEntity<>(ret, HttpStatus.OK);
    } catch (DataqueryException e) {
      log.error("Error while loading dataqueries of user {}", principal.getName(), e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping(path = "/by-user/{userId}")
  public ResponseEntity<Object> getDataqueriesByUserId(@PathVariable(value = "userId") String userId,
                                                       @RequestParam(value = "include-temporary", required = false, defaultValue = "false") boolean includeTemporary,
                                                       @RequestParam(value = "skip-validation", required = false, defaultValue = "false") boolean skipValidation) {

    try {
      var dataqueries = dataqueryHandler.getDataqueriesByAuthor(userId, includeTemporary);
      var ret = new ArrayList<Dataquery>();
      dataqueries.forEach(dq -> {
        ret.add(
            Dataquery.builder()
                .id(dq.id())
                .label(dq.label())
                .comment(dq.comment())
                .lastModified(dq.lastModified())
                .createdBy(dq.createdBy())
                .resultSize(dq.resultSize())
                .ccdl(ccdlSectionInfo(dq.content(), skipValidation))
                .dataExtraction(CrtdlSectionInfo.builder()
                    .exists(dq.content() != null && dq.content().dataExtraction() != null)
                    .isValid(true) // TODO: Add validation for that
                    .build())
                .expiresAt(dq.expiresAt())
                .build()
        );
      });
      return new ResponseEntity<>(ret, HttpStatus.OK);
    } catch (DataqueryException e) {
      log.error("Error while loading dataqueries of user {}", userId, e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping(path = "/by-user/{userId}")
  public ResponseEntity<Object> storeDataqueryForUser(@RequestBody Dataquery dataquery,
                                                      @PathVariable(value = "userId") String userId,
                                                      @RequestParam(value = "ttl") String ttlDuration) {

    Long dataqueryId;
    try {
      dataqueryId = dataqueryHandler.storeExpiringDataquery(dataquery, userId, ttlDuration);
    } catch (DataqueryException e) {
      log.error("Error while storing dataquery", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataqueryStorageFullException e) {
      return new ResponseEntity<>("storage exceeded", HttpStatus.FORBIDDEN);
    }

    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(dataqueryId)
        .toUri();

    return ResponseEntity
        .created(location)
        .build();
  }

  @PutMapping(path = "/{dataqueryId}")
  public ResponseEntity<Object> updateDataquery(@PathVariable(value = "dataqueryId") Long dataqueryId,
                                                @RequestBody Dataquery dataquery,
                                                Principal principal) {
    try {
      dataqueryHandler.updateDataquery(dataqueryId, dataquery, principal.getName());
      var dataquerySlots = dataqueryHandler.getDataquerySlotsJson(principal.getName());
      return new ResponseEntity<>(dataquerySlots, HttpStatus.OK);
    } catch (DataqueryException e) {
      log.debug("Dataquery with id {} not found or not accessible", dataqueryId, e);
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (JacksonException e) {
      log.warn("Could not serialize content of dataquery with id {}", dataqueryId, e);
      return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    } catch (DataqueryStorageFullException e) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
  }

  @DeleteMapping(path = "/{dataqueryId}")
  public ResponseEntity<Object> deleteDataquery(@PathVariable(value = "dataqueryId") Long dataqueryId,
                                                Principal principal) {
    try {
      dataqueryHandler.deleteDataquery(dataqueryId, principal.getName());
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (DataqueryException e) {
      log.debug("Dataquery with id {} not found or not accessible", dataqueryId, e);
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping("/query-slots")
  public ResponseEntity<Object> getDataquerySlots(Principal principal) {
    return new ResponseEntity<>(dataqueryHandler.getDataquerySlotsJson(principal.getName()), HttpStatus.OK);
  }

  private CrtdlSectionInfo ccdlSectionInfo(Crtdl content, boolean skipValidation) {
    var ccdl = content == null ? null : content.cohortDefinition();
    return CrtdlSectionInfo.builder()
        .exists(ccdl != null)
        .isValid(skipValidation || (ccdl != null && validationService.isValid(ccdl)))
        .build();
  }

  private CrtdlSectionInfo dataExtractionSectionInfo(Crtdl content, boolean skipValidation) {
    var dataExtraction = content == null ? null : content.dataExtraction();
    return CrtdlSectionInfo.builder()
        .exists(dataExtraction != null)
        .isValid(skipValidation || (dataExtraction != null && validationService.isValid(dataExtraction)))
        .build();
  }
}
