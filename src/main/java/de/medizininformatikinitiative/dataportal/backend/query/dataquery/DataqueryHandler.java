package de.medizininformatikinitiative.dataportal.backend.query.dataquery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.medizininformatikinitiative.dataportal.backend.query.api.Crtdl;
import de.medizininformatikinitiative.dataportal.backend.query.api.DataExtraction;
import de.medizininformatikinitiative.dataportal.backend.query.api.Dataquery;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.IssueWrapper;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.SavedQuerySlots;
import de.medizininformatikinitiative.dataportal.backend.query.api.validation.JsonSchemaValidator;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.DataqueryRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.threeten.extra.PeriodDuration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

@Slf4j
@Transactional
@RequiredArgsConstructor
public class DataqueryHandler {
  @NonNull
  final JsonSchemaValidator jsonSchemaValidator;
  @NonNull
  private final DataqueryCsvExportService csvExportHandler;
  @NonNull
  private ObjectMapper jsonUtil;
  @NonNull
  private DataqueryRepository dataqueryRepository;
  @NonNull
  private Integer maxDataqueriesPerUser;

  @NonNull
  private String keycloakAdminRole;

  public Long storeDataquery(@NonNull Dataquery dataquery, @NonNull String userId) throws DataqueryException, DataqueryStorageFullException {

    // By definition, a user can save an unlimited amount of queries without result
    if (dataquery.resultSize() != null && dataqueryRepository.countByCreatedByWhereResultIsNotNull(userId) >= maxDataqueriesPerUser) {
      throw new DataqueryStorageFullException();
    }

    var tmp = Dataquery.builder()
        .resultSize(dataquery.resultSize())
        .content(dataquery.content())
        .label(dataquery.label())
        .comment(dataquery.comment())
        .createdBy(userId)
        .build();

    try {
      de.medizininformatikinitiative.dataportal.backend.query.persistence.Dataquery dataqueryEntity = de.medizininformatikinitiative.dataportal.backend.query.persistence.Dataquery.of(tmp);
      dataqueryEntity = dataqueryRepository.save(dataqueryEntity);
      return dataqueryEntity.getId();
    } catch (JsonProcessingException e) {
      throw new DataqueryException(e.getMessage());
    }
  }

  public Long storeExpiringDataquery(@NonNull Dataquery dataquery, @NonNull String userId, @NonNull String ttlDuration) throws DataqueryException, DataqueryStorageFullException {

    var tmp = Dataquery.builder()
        .resultSize(dataquery.resultSize())
        .content(dataquery.content())
        .label(dataquery.label())
        .comment(dataquery.comment())
        .createdBy(userId)
        .expiresAt(Timestamp.valueOf(LocalDateTime.now().plusSeconds(PeriodDuration.parse(ttlDuration).getDuration().getSeconds())))
        .build();

    try {
      de.medizininformatikinitiative.dataportal.backend.query.persistence.Dataquery dataqueryEntity = de.medizininformatikinitiative.dataportal.backend.query.persistence.Dataquery.of(tmp);
      dataqueryEntity = dataqueryRepository.save(dataqueryEntity);
      return dataqueryEntity.getId();
    } catch (JsonProcessingException e) {
      throw new DataqueryException(e.getMessage());
    }
  }

  public Dataquery getDataqueryById(Long dataqueryId, Authentication userAuthentication) throws DataqueryException, JsonProcessingException {
    de.medizininformatikinitiative.dataportal.backend.query.persistence.Dataquery dataquery = dataqueryRepository.findById(dataqueryId).orElseThrow(DataqueryException::new);
    if (hasAccess(dataquery, userAuthentication)) {
      return Dataquery.of(dataquery);
    } else {
      throw new DataqueryException();
    }
  }

  public void updateDataquery(Long queryId, Dataquery dataquery, String userId) throws DataqueryException, DataqueryStorageFullException, JsonProcessingException {
    var usedSlots = dataqueryRepository.countByCreatedByWhereResultIsNotNull(userId);
    var existingDataquery = dataqueryRepository.findById(queryId).orElseThrow(DataqueryException::new);

    if (usedSlots >= maxDataqueriesPerUser) {
      // Only throw an exception when the updated query contains a result and the original didn't
      if (dataquery.resultSize() != null && existingDataquery.getResultSize() == null) {
        throw new DataqueryStorageFullException();
      }
    }

    if (existingDataquery.getCreatedBy().equals(userId)) {
      var dataqueryToUpdate = de.medizininformatikinitiative.dataportal.backend.query.persistence.Dataquery.of(dataquery);
      dataqueryToUpdate.setId(existingDataquery.getId());
      dataqueryToUpdate.setCreatedBy(userId);
      dataqueryToUpdate.setLastModified(Timestamp.valueOf(LocalDateTime.now()));
      dataqueryRepository.save(dataqueryToUpdate);
    } else {
      throw new DataqueryException();
    }
  }

  public List<Dataquery> getDataqueriesByAuthor(String userId, boolean includeTemporary) throws DataqueryException {
    List<de.medizininformatikinitiative.dataportal.backend.query.persistence.Dataquery> dataqueries;

    dataqueries = dataqueryRepository.findAllByCreatedBy(userId, includeTemporary);

    List<Dataquery> ret = new ArrayList<>();

    for (de.medizininformatikinitiative.dataportal.backend.query.persistence.Dataquery dataquery : dataqueries) {
      try {
        ret.add(Dataquery.of(dataquery));
      } catch (JsonProcessingException e) {
        throw new DataqueryException();
      }
    }

    return ret;
  }

  public List<Dataquery> getDataqueriesByAuthor(String userId) throws DataqueryException {
    return getDataqueriesByAuthor(userId, false);
  }

  public void deleteDataquery(Long dataqueryId, String userId) throws DataqueryException {
    de.medizininformatikinitiative.dataportal.backend.query.persistence.Dataquery dataquery = dataqueryRepository.findById(dataqueryId).orElseThrow(DataqueryException::new);
    if (!dataquery.getCreatedBy().equals(userId)) {
      throw new DataqueryException();
    } else {
      dataqueryRepository.delete(dataquery);
    }
  }

  public SavedQuerySlots getDataquerySlotsJson(String userId) {
    var queryAmount = dataqueryRepository.countByCreatedByWhereResultIsNotNull(userId);

    return SavedQuerySlots.builder()
        .used(queryAmount)
        .total(maxDataqueriesPerUser)
        .build();
  }

  public ByteArrayOutputStream createCsvExportZipfile(Dataquery dataquery) throws DataqueryException, IOException {
    if (dataquery.content() == null || dataquery.content().cohortDefinition() == null) {
      throw new DataqueryException("No ccdl part present");
    }
    var byteArrayOutputStream = new ByteArrayOutputStream();
    var zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
    Map<String, String> files = new HashMap<>();
    files.put("Datendefinition.json", jsonUtil.writeValueAsString(dataquery.content()));

    for (DataqueryCsvExportService.SUPPORTED_LANGUAGES lang : DataqueryCsvExportService.SUPPORTED_LANGUAGES.values()) {
      if (dataquery.content().cohortDefinition().inclusionCriteria() == null) {
        // Call with empty lists to just write the headers to the file
        files.put(MultiMessageBundle.getEntry("filenameInclusion", lang) + ".csv", csvExportHandler.jsonToCsv(List.of(List.of()), lang));
      } else {
        files.put(MultiMessageBundle.getEntry("filenameInclusion", lang) + ".csv", csvExportHandler.jsonToCsv(dataquery.content().cohortDefinition().inclusionCriteria(), lang));
      }
      if (dataquery.content().cohortDefinition().exclusionCriteria() == null) {
        // Call with empty lists to just write the headers to the file
        files.put(MultiMessageBundle.getEntry("filenameExclusion", lang) + ".csv", csvExportHandler.jsonToCsv(List.of(List.of()), lang));
      } else {
        files.put(MultiMessageBundle.getEntry("filenameExclusion", lang) + ".csv", csvExportHandler.jsonToCsv(dataquery.content().cohortDefinition().exclusionCriteria(), lang));
      }
      if (dataquery.content().dataExtraction() == null) {
        // Call with empty lists to just write the headers to the file
        files.put(MultiMessageBundle.getEntry("filenameFeatures", lang) + ".csv", csvExportHandler.jsonToCsv(DataExtraction.builder().build(), lang));
      } else {
        files.put(MultiMessageBundle.getEntry("filenameFeatures", lang) + ".csv", csvExportHandler.jsonToCsv(dataquery.content().dataExtraction(), lang));
      }
    }

    for (Map.Entry<String, String> file : files.entrySet()) {
      csvExportHandler.addFileToZip(zipOutputStream, file.getKey(), file.getValue());
    }

    zipOutputStream.close();
    byteArrayOutputStream.close();
    return byteArrayOutputStream;
  }

  public List<IssueWrapper> validateDataquery(JsonNode dataqueryNode) {
    List<IssueWrapper> issues = new ArrayList<>();
    var validationErrors = jsonSchemaValidator.validate(JsonSchemaValidator.SCHEMA_DATAQUERY, dataqueryNode);
    if (!validationErrors.isEmpty()) {
      issues = validationErrors.stream()
          .map(e -> new IssueWrapper(e.getInstanceLocation().toString(), e.getMessage()))
          .toList();
    }
    return issues;
  }

  public Dataquery dataqueryFromJsonNode(JsonNode jsonNode) {
    return jsonUtil.convertValue(jsonNode, Dataquery.class);
  }

  public List<IssueWrapper> validateCrtdl(JsonNode crtdlNode) {
    List<IssueWrapper> issues = new ArrayList<>();
    var validationErrors = jsonSchemaValidator.validate(JsonSchemaValidator.SCHEMA_CRTDL, crtdlNode);
    if (!validationErrors.isEmpty()) {
      issues = validationErrors.stream()
          .map(e -> new IssueWrapper(e.getInstanceLocation().toString(), e.getMessage()))
          .toList();
    }
    return issues;
  }

  public Crtdl crtdlFromJsonNode(JsonNode jsonNode) {
    return jsonUtil.convertValue(jsonNode, Crtdl.class);
  }

  public List<IssueWrapper> validateDataExtraction(JsonNode dataExtractionNode) {
    List<IssueWrapper> issues = new ArrayList<>();
    var validationErrors = jsonSchemaValidator.validate(JsonSchemaValidator.SCHEMA_DATAEXTRACTION, dataExtractionNode);
    if (!validationErrors.isEmpty()) {
      issues = validationErrors.stream()
          .map(e -> new IssueWrapper(e.getInstanceLocation().toString(), e.getMessage()))
          .toList();
    }
    return issues;
  }

  public DataExtraction dataExtractionFromJsonNode(JsonNode jsonNode) {
    return jsonUtil.convertValue(jsonNode, DataExtraction.class);
  }

  private boolean hasAccess(de.medizininformatikinitiative.dataportal.backend.query.persistence.Dataquery dataquery, Authentication authentication) {
    var creator = dataquery.getCreatedBy();
    if (creator == null || creator.isBlank()) {
      return false;
    }
    var isAdmin = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(role -> role.equals(keycloakAdminRole));
    return isAdmin || creator.equals(authentication.getName());
  }
}
