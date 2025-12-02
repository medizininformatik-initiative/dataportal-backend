package de.numcodex.feasibility_gui_backend.query.v5;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.numcodex.feasibility_gui_backend.query.api.Crtdl;
import de.numcodex.feasibility_gui_backend.query.api.CrtdlSectionInfo;
import de.numcodex.feasibility_gui_backend.query.api.Dataquery;
import de.numcodex.feasibility_gui_backend.query.dataquery.DataqueryCsvExportException;
import de.numcodex.feasibility_gui_backend.query.dataquery.DataqueryException;
import de.numcodex.feasibility_gui_backend.query.dataquery.DataqueryHandler;
import de.numcodex.feasibility_gui_backend.query.dataquery.DataqueryStorageFullException;
import de.numcodex.feasibility_gui_backend.terminology.validation.CcdlValidation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;

import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.*;

/*
Rest Interface for the UI to send and receive dataqueries from the backend.
*/
@RequestMapping(PATH_API + PATH_QUERY + PATH_DATA)
@RestController("DataqueryHandlerRestController-v5")
@Slf4j
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = "Location")
public class DataqueryHandlerRestController {
  private final static String API_VERSION = "v5";
  private final DataqueryHandler dataqueryHandler;
  private final CcdlValidation ccdlValidation;
  private final SmartValidator validator;
  private final String apiBaseUrl;

  public DataqueryHandlerRestController(DataqueryHandler dataqueryHandler,
                                        CcdlValidation ccdlValidation,
                                        SmartValidator validator,
                                        @Value("${app.apiBaseUrl}") String apiBaseUrl) {
    this.dataqueryHandler = dataqueryHandler;
    this.ccdlValidation = ccdlValidation;
    this.validator = validator;
    this.apiBaseUrl = apiBaseUrl;
  }

  @PostMapping(path = "")
  public ResponseEntity<Object> storeDataquery(@RequestBody Dataquery dataquery,
                                               @Context HttpServletRequest httpServletRequest, Principal principal) {

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

    UriComponentsBuilder uriBuilder = (apiBaseUrl != null && !apiBaseUrl.isEmpty())
        ? ServletUriComponentsBuilder.fromUriString(apiBaseUrl)
        : ServletUriComponentsBuilder.fromRequestUri(httpServletRequest);

    var uriString = uriBuilder.replacePath("")
        .pathSegment("api", API_VERSION, "query", "data", String.valueOf(dataqueryId))
        .build()
        .toUriString();
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.LOCATION, uriString);
    return new ResponseEntity<>(dataquerySlots, httpHeaders, HttpStatus.CREATED);
  }

  @GetMapping(path = "/{dataqueryId}")
  public ResponseEntity<Object> getDataquery(@PathVariable(value = "dataqueryId") Long dataqueryId,
                                                 @RequestParam(value = "skip-validation", required = false, defaultValue = "false") boolean skipValidation,
      Authentication authentication) {

    try {
      var dataquery = dataqueryHandler.getDataqueryById(dataqueryId, authentication);
      var dataqueryWithInvalidCriteria = Dataquery.builder()
              .id(dataquery.id())
              .content(
                  Crtdl.builder()
                      .display(dataquery.content().display())
                      .version(dataquery.content().version())
                      .dataExtraction(dataquery.content().dataExtraction())
                      .cohortDefinition(dataquery.content().cohortDefinition() == null ? null : ccdlValidation.annotateCcdl(dataquery.content().cohortDefinition(), skipValidation))
                      .build()
              )
              .label(dataquery.label())
              .comment(dataquery.comment())
              .lastModified(dataquery.lastModified())
              .createdBy(dataquery.createdBy())
              .resultSize(dataquery.resultSize())
              .ccdl(CrtdlSectionInfo.builder()
                  .exists(dataquery.content().cohortDefinition() != null)
                  .isValid(skipValidation || (dataquery.content().cohortDefinition() != null && ccdlValidation.isValid(dataquery.content().cohortDefinition())))
                  .build())
              .dataExtraction(CrtdlSectionInfo.builder()
                  .exists(dataquery.content().dataExtraction() != null)
                  .isValid(true) // TODO: Add validation for that
                  .build())
              .build();
      return new ResponseEntity<>(dataqueryWithInvalidCriteria, HttpStatus.OK);
    } catch (JsonProcessingException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataqueryException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping(path = "/{dataqueryId}" + PATH_CRTDL)
  public ResponseEntity<Object> getDataqueryCrtdl(@PathVariable(value = "dataqueryId") Long dataqueryId,
                                             @RequestParam(value = "skip-validation", required = false, defaultValue = "false") boolean skipValidation,
                                             Authentication authentication) {

    try {
      var dataquery = dataqueryHandler.getDataqueryById(dataqueryId, authentication);
      var crtdlWithInvalidCritiera = Crtdl.builder()
          .display(dataquery.content().display())
          .version(dataquery.content().version())
          .dataExtraction(dataquery.content().dataExtraction())
          .cohortDefinition(dataquery.content().cohortDefinition() == null ? null : ccdlValidation.annotateCcdl(dataquery.content().cohortDefinition(), skipValidation))
          .build();
      return new ResponseEntity<>(crtdlWithInvalidCritiera, HttpStatus.OK);
    } catch (JsonProcessingException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataqueryException e) {
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
      String headerValue = "attachment; filename=" + dataquery.label().toUpperCase() +  "_dataquery.zip";
      headers.add(HttpHeaders.CONTENT_DISPOSITION, headerValue);
      headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
      return new ResponseEntity<>(zipByteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);
    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataqueryException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (DataqueryCsvExportException e) {
      return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }

  @PostMapping(path = "/convert" + PATH_CRTDL)
  public ResponseEntity<Object> convertCrtdlToCsv(@RequestBody JsonNode crtdlNode,
                                                     Authentication authentication) {
    var validationErrors = dataqueryHandler.validateCrtdl(crtdlNode);
    if (!validationErrors.isEmpty()) {
      return new ResponseEntity<>(validationErrors, HttpStatus.BAD_REQUEST);
    }

    var crtdl = dataqueryHandler.crtdlFromJsonNode(crtdlNode);
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
    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataqueryException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (DataqueryCsvExportException e) {
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
                .ccdl(CrtdlSectionInfo.builder()
                    .exists(dq.content().cohortDefinition() != null)
                    .isValid(skipValidation || (dq.content().cohortDefinition() != null && ccdlValidation.isValid(dq.content().cohortDefinition())))
                    .build())
                .dataExtraction(CrtdlSectionInfo.builder()
                    .exists(dq.content().dataExtraction() != null)
                    .isValid(true) // TODO: Add validation for that
                    .build())
                .expiresAt(dq.expiresAt())
                .build()
        );
      });
      return new ResponseEntity<>(ret, HttpStatus.OK);
    } catch (DataqueryException e) {
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
                .ccdl(CrtdlSectionInfo.builder()
                    .exists(dq.content().cohortDefinition() != null)
                    .isValid(skipValidation || ccdlValidation.isValid(dq.content().cohortDefinition()))
                    .build())
                .dataExtraction(CrtdlSectionInfo.builder()
                    .exists(dq.content().dataExtraction() != null)
                    .isValid(true) // TODO: Add validation for that
                    .build())
                .expiresAt(dq.expiresAt())
                .build()
        );
      });
      return new ResponseEntity<>(ret, HttpStatus.OK);
    } catch (DataqueryException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping(path = "/by-user/{userId}")
  public ResponseEntity<Object> storeDataqueryForUser(@RequestBody Dataquery dataquery,
                                                      @PathVariable(value = "userId") String userId,
                                                      @RequestParam(value = "ttl") String ttlDuration,
                                                      @Context HttpServletRequest httpServletRequest) {

    Long dataqueryId;
    try {
      dataqueryId = dataqueryHandler.storeExpiringDataquery(dataquery, userId, ttlDuration);
    } catch (DataqueryException e) {
      log.error("Error while storing dataquery", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataqueryStorageFullException e) {
      return new ResponseEntity<>("storage exceeded", HttpStatus.FORBIDDEN);
    }

    UriComponentsBuilder uriBuilder = (apiBaseUrl != null && !apiBaseUrl.isEmpty())
        ? ServletUriComponentsBuilder.fromUriString(apiBaseUrl)
        : ServletUriComponentsBuilder.fromRequestUri(httpServletRequest);

    var uriString = uriBuilder.replacePath("")
        .pathSegment("api", API_VERSION, "query", "data", String.valueOf(dataqueryId))
        .build()
        .toUriString();
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.LOCATION, uriString);
    return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
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
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (JsonProcessingException e) {
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
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping("/query-slots")
  public ResponseEntity<Object> getDataquerySlots(Principal principal) {
    return new ResponseEntity<>(dataqueryHandler.getDataquerySlotsJson(principal.getName()), HttpStatus.OK);
  }

  @PostMapping("/validate")
  public ResponseEntity<Object> validateCcdl(
      @RequestBody JsonNode dataqueryJsonNode) throws MethodArgumentNotValidException, NoSuchMethodException {

    // Validate Schema
    var schemaValidationErrors = dataqueryHandler.validateDataquery(dataqueryJsonNode);
    if (!schemaValidationErrors.isEmpty()) {
      return new ResponseEntity<>(schemaValidationErrors, HttpStatus.BAD_REQUEST);
    }

    // Validate Content
    var dataquery = dataqueryHandler.dataqueryFromJsonNode(dataqueryJsonNode);
    var bindingResult = new BeanPropertyBindingResult(dataquery, "dataquery");
    validator.validate(dataquery, bindingResult);
    if (bindingResult.hasErrors()) {
      var methodParameter = new MethodParameter(this.getClass().getDeclaredMethod("validateCcdl", JsonNode.class), 0);
      throw new MethodArgumentNotValidException(methodParameter, bindingResult);
    }

    return new ResponseEntity<>(HttpStatus.OK);
  }
}
