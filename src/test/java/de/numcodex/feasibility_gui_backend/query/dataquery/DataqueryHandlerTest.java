package de.numcodex.feasibility_gui_backend.query.dataquery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.Error;
import com.networknt.schema.path.NodePath;
import com.networknt.schema.path.PathType;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.*;
import de.numcodex.feasibility_gui_backend.query.api.Dataquery;
import de.numcodex.feasibility_gui_backend.query.api.validation.JsonSchemaValidator;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("query")
@Tag("peristence")
@ExtendWith(MockitoExtension.class)
class DataqueryHandlerTest {

    public static final String CREATOR = "creator-557261";
    public static final String LABEL = "some-label";
    public static final String COMMENT = "some-comment";
    public static final String TIME_STRING = "1969-07-20T20:17:40.0";
    public static final String EXPIRY_STRING = "2063-04-05 20:17:40.0";
    private static final int MAX_QUERIES_PER_USER = 5;
    private static final String KEYCLOAK_ADMIN_ROLE = "ROLE_DATAPORTAL_TEST_ADMIN";
    public static final String DURATION = "PT10M";

    private DataqueryHandler dataqueryHandler;

    @Spy
    private ObjectMapper jsonUtil = new ObjectMapper();

    @Mock
    private DataqueryRepository dataqueryRepository;

    @Mock
    private DataqueryCsvExportService csvExportService;

    @Mock
    private JsonSchemaValidator jsonSchemaValidator;

    void setMockAuth(String userName, List<String> roles) {
        Authentication mockAuth = Mockito.mock(Authentication.class);
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        lenient().doReturn(userName).when(mockAuth).getName();
        lenient().doReturn(authorities).when(mockAuth).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(mockAuth);
    }

    @BeforeEach
    void setUp() {
      jsonUtil.registerModule(new JavaTimeModule());
      jsonUtil.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      dataqueryHandler = new DataqueryHandler(jsonUtil, dataqueryRepository, csvExportService, jsonSchemaValidator, MAX_QUERIES_PER_USER, KEYCLOAK_ADMIN_ROLE);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("storeDataquery() -> trying to store a valid object with a user does not throw")
    void storeDataquery_succeeds() throws JsonProcessingException {
        doReturn(createDataqueryEntity()).when(dataqueryRepository).save(any());

        assertDoesNotThrow(() -> dataqueryHandler.storeDataquery(createDataquery(), CREATOR));
    }

    @Test
    @DisplayName("storeDataquery() -> trying to store a null object with a null user throws an exception")
    void storeDataquery_throwsOnEmptyQueryAndEmptyUser() {
        assertThrows(NullPointerException.class, () -> dataqueryHandler.storeDataquery(null, null));
    }

    @Test
    @DisplayName("storeDataquery() -> trying to store a null object with a user throws an exception")
    void storeDataquery_throwsOnEmptyQueryAndNonEmptyUser() {
        assertThrows(NullPointerException.class, () -> dataqueryHandler.storeDataquery(null, CREATOR));
    }

    @Test
    @DisplayName("storeDataquery() -> trying to store an object with a null user throws an exception")
    void storeDataquery_throwsOnNonEmptyQueryAndEmptyUser() {
        assertThrows(NullPointerException.class, () -> dataqueryHandler.storeDataquery(createDataquery(), null));
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    @DisplayName("storeDataquery() -> trying to store a dataquery when no slots are free throws")
    void storeDataquery_throwsOnNoFreeSlots(boolean withResult) throws JsonProcessingException {
        lenient().doReturn(MAX_QUERIES_PER_USER + 1L).when(dataqueryRepository).countByCreatedByWhereResultIsNotNull(any(String.class));
        lenient().doReturn(createDataqueryEntity()).when(dataqueryRepository).save(any(de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.class));

        if (withResult) {
            assertThrows(DataqueryStorageFullException.class, () -> dataqueryHandler.storeDataquery(createDataquery(withResult), CREATOR));
        } else {
            assertDoesNotThrow(() -> dataqueryHandler.storeDataquery(createDataquery(withResult), CREATOR));
        }
    }

    @ParameterizedTest
    @CsvSource({"true,-1", "true,0", "true,1", "false,-1", "false,0", "false,1"})
    @DisplayName("storeDataquery() -> checking around the query limit")
    void storeDataquery_testFreeSlotOnEdgeCases(boolean withResult, long offset) throws JsonProcessingException {
        lenient().doReturn(MAX_QUERIES_PER_USER + offset).when(dataqueryRepository).countByCreatedByWhereResultIsNotNull(any(String.class));
        lenient().doReturn(createDataqueryEntity()).when(dataqueryRepository).save(any(de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.class));

        if (withResult) {
            if (offset < 0) {
                assertDoesNotThrow(() -> dataqueryHandler.storeDataquery(createDataquery(withResult), CREATOR));
            } else {
                assertThrows(DataqueryStorageFullException.class, () -> dataqueryHandler.storeDataquery(createDataquery(withResult), CREATOR));
            }
        } else {
            assertDoesNotThrow(() -> dataqueryHandler.storeDataquery(createDataquery(withResult), CREATOR));
        }
    }

    @Test
    @DisplayName("storeDataquery() -> error in json serialization throws an exception")
    void storeDataquery_throwsOnJsonSerializationError() throws JsonProcessingException {
        var dataquery = createDataquery();

        try (MockedStatic<de.numcodex.feasibility_gui_backend.query.persistence.Dataquery> mockedStaticDataquery
                 = mockStatic(de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.class)) {
            mockedStaticDataquery.when(() -> de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.of(any(Dataquery.class))).thenThrow(JsonProcessingException.class);
            assertThrows(DataqueryException.class, () -> dataqueryHandler.storeDataquery(dataquery, CREATOR));
        }
    }

    @Test
    @DisplayName("getDataqueryById() -> retrieving a single dataquery by its id succeeds")
    void getDataqueryById_succeeds() throws JsonProcessingException {
        setMockAuth(CREATOR, List.of("DATAPORTAL_USER"));
        var dataqueryEntity = createDataqueryEntity();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        Dataquery dataquery = assertDoesNotThrow(() -> dataqueryHandler.getDataqueryById(1L, auth));
        assertNotNull(dataquery);
        assertInstanceOf(Dataquery.class, dataquery);
    }

    @Test
    @DisplayName("getDataqueryById() -> Throw exception if the user is not the author")
    void getDataqueryById_throwsOnWrongUser() throws JsonProcessingException {
        setMockAuth("NOT_THE"+ CREATOR, List.of("DATAPORTAL_USER"));
        var dataqueryEntity = createDataqueryEntity();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        assertThrows(DataqueryException.class, () -> dataqueryHandler.getDataqueryById(1L, auth));
    }

    @Test
    @DisplayName("getDataqueryById() -> Succeeds for admin user")
    void getDataqueryById_succeedsForAdmin() throws JsonProcessingException {
        setMockAuth("NOT_THE"+ CREATOR, List.of("ROLE_DATAPORTAL_TEST_ADMIN"));
        var dataqueryEntity = createDataqueryEntity();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        Dataquery dataquery = assertDoesNotThrow(() -> dataqueryHandler.getDataqueryById(1L, auth));
        assertNotNull(dataquery);
        assertInstanceOf(Dataquery.class, dataquery);
    }

    @Test
    @DisplayName("getDataqueryById() -> Throw exception if the query does not exist")
    @WithMockUser(username = "NOT THE " + CREATOR, roles = {"DATAPORTAL_USER"})
    void getDataqueryById_throwsOnNotFound() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        doReturn(Optional.empty()).when(dataqueryRepository).findById(any(Long.class));

        assertThrows(DataqueryException.class, () -> dataqueryHandler.getDataqueryById(1L, auth));
    }

    @Test
    @DisplayName("updateDataquery() -> trying to update a dataquery succeeds")
    void updateDataquery_succeeds() throws JsonProcessingException {
        var dataquery = createDataquery();
        var dataqueryEntity = createDataqueryEntity();

        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        assertDoesNotThrow(() -> dataqueryHandler.updateDataquery(1L, dataquery, CREATOR));
    }

    @Test
    @DisplayName("updateDataquery() -> throws exception when actor is not the original creator")
    void updateDataquery_throwsOnWrongUser() throws JsonProcessingException {
        var dataquery = createDataquery();
        var dataqueryEntity = createDataqueryEntity();
        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        assertThrows(DataqueryException.class, () -> dataqueryHandler.updateDataquery(1L, dataquery, "NOT THE " + CREATOR));
    }

    @Test
    @DisplayName("updateDataquery() -> throws exception when query is not found")
    void updateDataquery_throwsOnNotFound() {
        doReturn(Optional.empty()).when(dataqueryRepository).findById(any(Long.class));

        assertThrows(DataqueryException.class, () -> dataqueryHandler.updateDataquery(1L, createDataquery(), CREATOR));
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    @DisplayName("updateDataquery() -> check if storage full exceptions are thrown correctly")
    void updateDataquery_throwsOnNoFreeSlots(boolean withResultNew, boolean withResultOld) throws JsonProcessingException {
        var dataquery = createDataquery(withResultNew);
        var dataqueryEntity = createDataqueryEntity(withResultOld);

        lenient().doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));
        lenient().doReturn((long) MAX_QUERIES_PER_USER).when(dataqueryRepository).countByCreatedByWhereResultIsNotNull(any(String.class));
        lenient().doReturn(createDataqueryEntity()).when(dataqueryRepository).save(any(de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.class));

        // When the new dataquery has no result, this should never throw. It should only throw if the old had no result and the new has a result
        if (withResultNew && !withResultOld) {
            assertThrows(DataqueryStorageFullException.class, () -> dataqueryHandler.updateDataquery(1L, dataquery, CREATOR));
        } else {
            assertDoesNotThrow(() -> dataqueryHandler.updateDataquery(1L, dataquery, CREATOR));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "-1,true,true", "-1,true,false", "-1,false,true", "-1,false,false",
        "0,true,true", "0,true,false", "0,false,true", "0,false,false",
        "1,true,true", "1,true,false", "1,false,true", "1,false,false"
    })
    @DisplayName("updateDataquery() -> checking around the query limit")
    void updateDataquery_testFreeSlotOnEdgeCases(long offset, boolean withResultNew, boolean withResultOld) throws JsonProcessingException {
        var dataquery = createDataquery(withResultNew);
        var dataqueryEntity = createDataqueryEntity(withResultOld);

        lenient().doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));
        lenient().doReturn(MAX_QUERIES_PER_USER + offset).when(dataqueryRepository).countByCreatedByWhereResultIsNotNull(any(String.class));
        lenient().doReturn(createDataqueryEntity()).when(dataqueryRepository).save(any(de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.class));

        // It should only throw when the new query has a result, the old didn't have one and the storage is full
        // If the storage is full but the old query already had a result, it should not fail.
        if (withResultNew && !withResultOld && offset >= 0) {
            assertThrows(DataqueryStorageFullException.class, () -> dataqueryHandler.updateDataquery(1L, dataquery, CREATOR));
        } else {
            assertDoesNotThrow(() -> dataqueryHandler.updateDataquery(1L, dataquery, CREATOR));
        }
    }

    @ParameterizedTest
    @DisplayName("getDataqueriesByAuthor() -> succeeds")
    @ValueSource(strings = { "true", "false" })
    void getDataqueriesByAuthor_succeedsWithEntry(boolean includeTemporary) throws JsonProcessingException {
        var dataqueryEntity = createDataqueryEntity();

        doReturn(List.of(dataqueryEntity)).when(dataqueryRepository).findAllByCreatedBy(any(String.class), anyBoolean());

        List<Dataquery> dataqueries = assertDoesNotThrow(() -> dataqueryHandler.getDataqueriesByAuthor(CREATOR, includeTemporary));

        assertNotNull(dataqueries);
        assertEquals(1, dataqueries.size());
        assertEquals(dataqueryEntity.getLabel(), dataqueries.get(0).label());
        assertEquals(dataqueryEntity.getComment(), dataqueries.get(0).comment());
    }

    @Test
    @DisplayName("getDataqueriesByAuthor() -> succeeds with empty list")
    void getDataqueriesByAuthor_succeedsWithEmptyList() throws JsonProcessingException {

        doReturn(List.of()).when(dataqueryRepository).findAllByCreatedBy(any(String.class), anyBoolean());

        List<Dataquery> dataqueries = assertDoesNotThrow(() -> dataqueryHandler.getDataqueriesByAuthor(CREATOR));

        assertNotNull(dataqueries);
        assertEquals(0, dataqueries.size());
    }

    @ParameterizedTest
    @ValueSource(strings = { "true", "false" })
    @DisplayName("getDataqueriesByAuthor() -> throws on json error")
    void getDataqueriesByAuthor_throwsOnJsonException(boolean includeTemporary) throws JsonProcessingException {
        var dataqueryEntity = createDataqueryEntity();

        try (MockedStatic<Dataquery> mockedStaticDataquery = mockStatic(Dataquery.class)) {
            mockedStaticDataquery.when(() -> Dataquery.of(any(de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.class))).thenThrow(JsonProcessingException.class);
            doReturn(List.of(dataqueryEntity)).when(dataqueryRepository).findAllByCreatedBy(any(String.class), anyBoolean());
            assertThrows(DataqueryException.class, () -> dataqueryHandler.getDataqueriesByAuthor(CREATOR, includeTemporary));
        }

    }

    @Test
    @DisplayName("deleteDataquery() -> succeeds")
    void deleteDataquery_succeeds() throws JsonProcessingException {
        var dataqueryEntity = createDataqueryEntity();

        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        assertDoesNotThrow(() -> dataqueryHandler.deleteDataquery(1L, CREATOR));
    }

    @Test
    @DisplayName("deleteDataquery() -> throws on wrong user")
    void deleteDataquery_throwsOnWrongUser() throws JsonProcessingException {
        var dataqueryEntity = createDataqueryEntity();

        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        assertThrows(DataqueryException.class, () -> dataqueryHandler.deleteDataquery(1L, "NOT THE " +CREATOR));
    }

    @Test
    @DisplayName("getDataquerySlotsJson() -> succeeds")
    void getDataquerySlotsJson_succeeds() throws JsonProcessingException {
        var usedSlots = 7L;

        doReturn(usedSlots).when(dataqueryRepository).countByCreatedByWhereResultIsNotNull(any(String.class));

        var savedQuerySlots = assertDoesNotThrow(() -> dataqueryHandler.getDataquerySlotsJson(CREATOR));

        assertEquals(usedSlots, savedQuerySlots.used());
    }

    @Test
    @DisplayName("convertApiToPersistence() -> converting a dataquery from the rest api to the format that will be stored in the database succeeds")
    void testDataqueryPersistenceOfDataQueryApi() throws JsonProcessingException {
        var dataQuery = createDataquery();

        var convertedDataquery = de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.of(dataQuery);
        var convertedCrtdl = jsonUtil.readValue(convertedDataquery.getCrtdl(), Crtdl.class);

        assertEquals(convertedDataquery.getId(), dataQuery.id());
        assertEquals(convertedDataquery.getLabel(), dataQuery.label());
        assertEquals(convertedDataquery.getComment(), dataQuery.comment());
        assertEquals(convertedDataquery.getLastModified().toString(), dataQuery.lastModified());
        assertEquals(convertedDataquery.getResultSize(), dataQuery.resultSize());
        assertEquals(convertedCrtdl.display(), dataQuery.content().display());
        assertEquals(convertedCrtdl.version(), dataQuery.content().version());
        assertEquals(convertedCrtdl.cohortDefinition().display(), dataQuery.content().cohortDefinition().display());
        assertEquals(convertedCrtdl.cohortDefinition().version(), dataQuery.content().cohortDefinition().version());
        assertEquals(convertedCrtdl.cohortDefinition().inclusionCriteria(), dataQuery.content().cohortDefinition().inclusionCriteria());
        assertEquals(convertedCrtdl.cohortDefinition().exclusionCriteria(), dataQuery.content().cohortDefinition().exclusionCriteria());
    }

    @Test
    @DisplayName("Dataquery.of() -> converting a dataquery from the database to the format that will be sent out via api succeeds")
    void testDataqueryApiOfDataQueryPersistence() throws JsonProcessingException {
        var dataqueryEntity = createDataqueryEntity();
        var convertedDataquery = Dataquery.of(dataqueryEntity);
        var originalCrtdl = jsonUtil.readValue(dataqueryEntity.getCrtdl(), Crtdl.class);

        assertEquals(convertedDataquery.id(), dataqueryEntity.getId());
        assertEquals(convertedDataquery.label(), dataqueryEntity.getLabel());
        assertEquals(convertedDataquery.comment(), dataqueryEntity.getComment());
        assertEquals(convertedDataquery.createdBy(), dataqueryEntity.getCreatedBy());
        assertEquals(convertedDataquery.lastModified(), dataqueryEntity.getLastModified().toString());
        assertEquals(convertedDataquery.content().display(), originalCrtdl.display());
        assertEquals(convertedDataquery.content().version(), originalCrtdl.version());
        assertEquals(convertedDataquery.content().cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).code(),
            originalCrtdl.cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).code());
        assertEquals(convertedDataquery.content().cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).display(),
            originalCrtdl.cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).display());
        assertEquals(convertedDataquery.content().cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).version(),
            originalCrtdl.cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).version());
        assertEquals(convertedDataquery.content().cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).system(),
            originalCrtdl.cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).system());
        assertEquals(convertedDataquery.content().cohortDefinition().exclusionCriteria(), originalCrtdl.cohortDefinition().exclusionCriteria());
    }

    @Test
    @DisplayName("storeExpiringDataquery() -> trying to store a valid object with a user does not throw")
    void storeExpiringDataquery_succeeds() throws JsonProcessingException {
        doReturn(createDataqueryEntity(false, true)).when(dataqueryRepository).save(any());

        assertDoesNotThrow(() -> dataqueryHandler.storeExpiringDataquery(createDataquery(), CREATOR, DURATION));
    }

    @ParameterizedTest
    @CsvSource({"true,true,true", "true,true,false", "true,false,true", "true,false,false",
        "false,true,true", "false,true,false", "false,false,true"})
    @DisplayName("storeExpiringDataquery() -> trying to store a null object with a null user throws an exception")
    void storeExpiringDataquery_throwsOnEmptyValue(boolean emptyQuery, boolean emptyUser, boolean emptyTtl) throws JsonProcessingException {
        assertThrows(NullPointerException.class, () -> dataqueryHandler.storeExpiringDataquery(
            emptyQuery ? null : createDataquery(),
            emptyUser ? null : CREATOR,
            emptyTtl ? null : DURATION)
        );
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    @DisplayName("storeExpiringDataquery() -> trying to store an expiring dataquery when no slots are free does not throw")
    void storeExpiringDataquery_ignoresFreeSlots(boolean withResult) throws JsonProcessingException {
        lenient().doReturn(MAX_QUERIES_PER_USER + 1L).when(dataqueryRepository).countByCreatedByWhereResultIsNotNull(any(String.class));
        lenient().doReturn(createDataqueryEntity()).when(dataqueryRepository).save(any(de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.class));

        assertDoesNotThrow(() -> dataqueryHandler.storeExpiringDataquery(createDataquery(withResult), CREATOR, DURATION));
    }


    @ParameterizedTest
    @CsvSource({"true,true,true", "true,true,false", "true,false,true", "true,false,false", "false,true,true",
        "false,true,false", "false,false,true", "false,false,false"})
    @DisplayName("createCsvExportZipfile() -> creating a csv export succeeds")
    void createCsvExportZipfile(String withInclusionCriteria, String withExclusionCriteria, String withDataextraction) {
        var dataquery = createDataquery(Boolean.parseBoolean(withInclusionCriteria),
            Boolean.parseBoolean(withExclusionCriteria),
            Boolean.parseBoolean(withDataextraction));

        var byteArrayOutputStream = assertDoesNotThrow(() -> dataqueryHandler.createCsvExportZipfile(dataquery));
        assertNotNull(byteArrayOutputStream);
        assertInstanceOf(ByteArrayOutputStream.class, byteArrayOutputStream);
    }

    @Test
    @DisplayName("createCsvExportZipfile() -> creating a csv export fails if no content is set")
    void createCsvExportZipfile_throwsOnNullContent() {
        var dataqueryWithoutContent = Dataquery.builder()
            .id(1L)
            .label(LABEL)
            .comment(COMMENT)
            .createdBy(CREATOR)
            .lastModified(Timestamp.valueOf(TIME_STRING))
            .build();

        assertThrows(DataqueryException.class, () -> dataqueryHandler.createCsvExportZipfile(dataqueryWithoutContent));
    }

    @Test
    @DisplayName("createCsvExportZipfile() -> creating a csv export fails if no cohort definition is set")
    void createCsvExportZipfile_throwsOnNullCohortDefinition() {
        var dataqueryWithoutCohortDefinition = Dataquery.builder()
            .id(1L)
            .content(Crtdl.builder()
                .display("foo")
                .build())
            .label(LABEL)
            .comment(COMMENT)
            .createdBy(CREATOR)
            .lastModified(Timestamp.valueOf(TIME_STRING))
            .build();

        assertThrows(DataqueryException.class, () -> dataqueryHandler.createCsvExportZipfile(dataqueryWithoutCohortDefinition));
    }

    @Test
    public void testValidateCrtdl_noErrors() throws JsonProcessingException {
      JsonNode jsonNode = jsonUtil.readTree("{\"foo\":\"bar\"}");
      doReturn(List.of()).when(jsonSchemaValidator).validate(any(String.class), any(JsonNode.class));

      var errors = dataqueryHandler.validateCrtdl(jsonNode);

      assertThat(errors).isEmpty();
    }

    @Test
    public void testValidateCrtdl_errors() throws JsonProcessingException {
      JsonNode jsonNode = jsonUtil.readTree("{\"foo\":\"bar\"}");
      doReturn(List.of(Error.builder().message("error").instanceLocation(new NodePath(PathType.DEFAULT)).build())).when(jsonSchemaValidator).validate(any(String.class), any(JsonNode.class));

      var errors = dataqueryHandler.validateCrtdl(jsonNode);

      assertThat(errors).isNotEmpty();
      assertThat(errors.size()).isEqualTo(1);
    }
    @Test
    public void testValidateDataExtraction_noErrors() throws JsonProcessingException {
      JsonNode jsonNode = jsonUtil.readTree("{\"foo\":\"bar\"}");
      doReturn(List.of()).when(jsonSchemaValidator).validate(any(String.class), any(JsonNode.class));

      var errors = dataqueryHandler.validateDataExtraction(jsonNode);

      assertThat(errors).isEmpty();
    }

    @Test
    public void testValidateDataExtraction_errors() throws JsonProcessingException {
      JsonNode jsonNode = jsonUtil.readTree("{\"foo\":\"bar\"}");
      doReturn(List.of(Error.builder().message("error").instanceLocation(new NodePath(PathType.DEFAULT)).build())).when(jsonSchemaValidator).validate(any(String.class), any(JsonNode.class));

      var errors = dataqueryHandler.validateDataExtraction(jsonNode);

      assertThat(errors).isNotEmpty();
      assertThat(errors.size()).isEqualTo(1);
    }

    @Test
    public void testCrtdlFromJsonNode_succeeds() throws Exception {
      JsonNode jsonNode = loadJson("/de/numcodex/feasibility_gui_backend/query/api/validation/crtdl-valid.json");

      var result = assertDoesNotThrow(() -> dataqueryHandler.crtdlFromJsonNode(jsonNode));
      assertThat(result).isInstanceOf(Crtdl.class);
    }

    @Test
    public void testCrtdlFromJsonNode_throwsOnInvalidJson() throws Exception {
      JsonNode jsonNode = loadJson("/de/numcodex/feasibility_gui_backend/query/api/validation/crtdl-invalid.json");

      Assertions.assertThrows(IllegalArgumentException.class, () -> dataqueryHandler.crtdlFromJsonNode(jsonNode));
    }

    @Test
    public void testDataExtractionFromJsonNode_succeeds() throws Exception {
      JsonNode jsonNode = loadJson("/de/numcodex/feasibility_gui_backend/query/api/validation/dataExtraction-valid.json");

      var result = assertDoesNotThrow(() -> dataqueryHandler.dataExtractionFromJsonNode(jsonNode));
      assertThat(result).isInstanceOf(DataExtraction.class);
    }

    @Test
    public void testDataExtractionFromJsonNode_throwsOnInvalidJson() throws Exception {
      JsonNode jsonNode = loadJson("/de/numcodex/feasibility_gui_backend/query/api/validation/dataExtraction-invalid.json");

      Assertions.assertThrows(IllegalArgumentException.class, () -> dataqueryHandler.dataExtractionFromJsonNode(jsonNode));
    }

    private JsonNode loadJson(String resourcePath) throws Exception {
      try (InputStream is = DataqueryHandlerTest.class.getResourceAsStream(resourcePath)) {
        if (is == null) {
          throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        return jsonUtil.readTree(is);
      }
    }

    private Dataquery createDataquery(boolean withInclusion, boolean withExclusion, boolean withExtraction) {
        return Dataquery.builder()
            .id(1L)
            .label(LABEL)
            .comment(COMMENT)
            .content(createCrtdl(withInclusion, withExclusion, withExtraction))
            .createdBy(CREATOR)
            .lastModified(Timestamp.valueOf(TIME_STRING))
            .build();
    }

    private Dataquery createDataquery(boolean withResult) {
        return Dataquery.builder()
            .id(1L)
            .label(LABEL)
            .comment(COMMENT)
            .content(createCrtdl())
            .resultSize(withResult ? 123L : null)
            .createdBy(CREATOR)
            .lastModified(Timestamp.valueOf(TIME_STRING))
            .build();
    }

    private Dataquery createDataquery() {
        return createDataquery(false);
    }

    private Crtdl createCrtdl(boolean withInclusion, boolean withExclusion, boolean withExtraction) {
        return Crtdl.builder()
            .cohortDefinition(createValidStructuredQuery(withInclusion, withExclusion))
            .dataExtraction(withExtraction ? createValidDataExtraction() : null)
            .display("foo")
            .build();
    }

    private DataExtraction createValidDataExtraction() {
        return DataExtraction.builder()
            .attributeGroups(List.of(createAttributeGroup()))
            .build();
    }

    private AttributeGroup createAttributeGroup() {
        return AttributeGroup.builder()
            .groupReference(URI.create("https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab"))
            .name("testgroup")
            .id("my-grp")
            .attributes(List.of(
                Attribute.builder().attributeRef("Observation.identifier").build(),
                Attribute.builder().attributeRef("Observation.status").build(),
                Attribute.builder().attributeRef("Observation.category").build(),
                Attribute.builder().attributeRef("Observation.code").build(),
                Attribute.builder().attributeRef("Observation.effective[x]").build()
            ))
            .build();
    }

    private Crtdl createCrtdl() {
        return Crtdl.builder()
            .cohortDefinition(createValidStructuredQuery())
            .display("foo")
            .build();
    }

    private StructuredQuery createValidStructuredQuery(boolean withInclusion, boolean withExclusion) {
        var termCode = TermCode.builder()
            .code("LL2191-6")
            .system("http://loinc.org")
            .display("Geschlecht")
            .build();
        var criterion = Criterion.builder()
            .termCodes(List.of(termCode))
            .attributeFilters(List.of())
            .build();
        return StructuredQuery.builder()
            .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
            .inclusionCriteria(withInclusion ? List.of(List.of(criterion)) : null)
            .exclusionCriteria(withExclusion ? List.of(List.of(criterion)) : null)
            .display("foo")
            .build();
    }

    private StructuredQuery createValidStructuredQuery() {
        var termCode = TermCode.builder()
                .code("LL2191-6")
                .system("http://loinc.org")
                .display("Geschlecht")
                .build();
        var inclusionCriterion = Criterion.builder()
                .termCodes(List.of(termCode))
                .attributeFilters(List.of())
                .build();
        return StructuredQuery.builder()
                .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
                .inclusionCriteria(List.of(List.of(inclusionCriterion)))
                .exclusionCriteria(null)
                .display("foo")
                .build();
    }

    private de.numcodex.feasibility_gui_backend.query.persistence.Dataquery createDataqueryEntity(boolean withResult, boolean expiring) throws JsonProcessingException {
        de.numcodex.feasibility_gui_backend.query.persistence.Dataquery out = new de.numcodex.feasibility_gui_backend.query.persistence.Dataquery();
        out.setId(1L);
        out.setLabel(LABEL);
        out.setComment(COMMENT);
        out.setLastModified(Timestamp.valueOf(TIME_STRING));
        out.setCreatedBy(CREATOR);
        out.setResultSize(withResult ? 123L : null);
        out.setExpiresAt(expiring ? Timestamp.valueOf(EXPIRY_STRING) : null);
        out.setCrtdl(jsonUtil.writeValueAsString(createCrtdl()));
        return out;
    }

    private de.numcodex.feasibility_gui_backend.query.persistence.Dataquery createDataqueryEntity(boolean withResult) throws JsonProcessingException {
        return  createDataqueryEntity(withResult, false);
    }

    private de.numcodex.feasibility_gui_backend.query.persistence.Dataquery createDataqueryEntity() throws JsonProcessingException {
        return  createDataqueryEntity(false, false);
    }
}
