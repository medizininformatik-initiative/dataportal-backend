package de.numcodex.feasibility_gui_backend.query.v5;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.query.api.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.status.SavedQuerySlots;
import de.numcodex.feasibility_gui_backend.query.api.status.ValidationIssue;
import de.numcodex.feasibility_gui_backend.query.dataquery.DataqueryException;
import de.numcodex.feasibility_gui_backend.query.dataquery.DataqueryHandler;
import de.numcodex.feasibility_gui_backend.query.dataquery.DataqueryStorageFullException;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.AuthenticationHelper;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.terminology.validation.StructuredQueryValidation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.sql.Timestamp;
import java.util.*;

import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MimeType;

import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("query")
@ExtendWith(SpringExtension.class)
@Import(RateLimitingServiceSpringConfig.class)
@WebMvcTest(
        controllers = DataqueryHandlerRestController.class
)
public class DataqueryHandlerRestControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper jsonUtil;

    @MockitoBean
    private DataqueryHandler dataqueryHandler;

    @MockitoBean
    private StructuredQueryValidation structuredQueryValidation;

    @MockitoBean
    private AuthenticationHelper authenticationHelper;

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testStoreDataquery_succeedsWith201() throws Exception {
        long queryId = 1L;
        doReturn(queryId).when(dataqueryHandler).storeDataquery(any(Dataquery.class), any(String.class));
        doReturn(createSavedQuerySlots()).when(dataqueryHandler).getDataquerySlotsJson(any(String.class));

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + PATH_DATA)).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(createValidDataqueryToStore(queryId))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("location"))
                .andExpect(header().string("location", PATH_API + PATH_QUERY + PATH_DATA + "/" + queryId))
                .andExpect(jsonPath("$.used").exists())
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testStoreDataqueryExceptionWith500() throws Exception {
        doThrow(DataqueryException.class).when(dataqueryHandler).storeDataquery(any(Dataquery.class), any(String.class));

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + PATH_DATA)).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(createValidDataqueryToStore(1L))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testStoreDataqueryExceptionWith403() throws Exception {
        doThrow(DataqueryStorageFullException.class).when(dataqueryHandler).storeDataquery(any(Dataquery.class), any(String.class));

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + PATH_DATA)).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(createValidDataqueryToStore(1L))))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataquery_succeeds() throws Exception {
        long dataqueryId = 1L;
        var annotatedQuery = createValidAnnotatedStructuredQuery(false);

        doReturn(createValidApiDataqueryToGet(dataqueryId)).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));
        doReturn(annotatedQuery).when(structuredQueryValidation).annotateStructuredQuery(any(StructuredQuery.class), any(Boolean.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/" + dataqueryId)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dataqueryId));
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataquery_failsOnNotFound() throws Exception {
        long dataqueryId = 1;

        doThrow(DataqueryException.class).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/" + dataqueryId)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataquery_failsOnJsonError() throws Exception {
        long dataqueryId = 1;

        doThrow(JsonProcessingException.class).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/" + dataqueryId)).with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataqueryCrtdl_succeeds() throws Exception {
        long dataqueryId = 1L;
        var annotatedQuery = createValidAnnotatedStructuredQuery(false);

        doReturn(createValidApiDataqueryToGet(dataqueryId)).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));
        doReturn(annotatedQuery).when(structuredQueryValidation).annotateStructuredQuery(any(StructuredQuery.class), any(Boolean.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/" + dataqueryId + "/crtdl")).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cohortDefinition.display").value(annotatedQuery.display()));
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataqueryCrtdl_failsOnNotFound() throws Exception {
        long dataqueryId = 1;

        doThrow(DataqueryException.class).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/" + dataqueryId + "/crtdl")).with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataqueryCrtdl_failsOnJsonError() throws Exception {
        long dataqueryId = 1;

        doThrow(JsonProcessingException.class).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/" + dataqueryId + "/crtdl")).with(csrf()))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataqueryCrtdlCsv_succeeds() throws Exception {
        long dataqueryId = 1L;

        doReturn(createValidApiDataqueryToGet(dataqueryId)).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));
        doReturn(createValidByteArrayOutputStream()).when(dataqueryHandler).createCsvExportZipfile(any(Dataquery.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/" + dataqueryId + "/crtdl"))
                .header(HttpHeaders.ACCEPT, "application/zip")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/zip"))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"))
            .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, Matchers.not("0")));
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataqueryCrtdlCsv_failsOnNotFound() throws Exception {
        long dataqueryId = 1;

        doReturn(createValidApiDataqueryToGet(dataqueryId)).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));
        doThrow(DataqueryException.class).when(dataqueryHandler).createCsvExportZipfile(any(Dataquery.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/" + dataqueryId + "/crtdl"))
                .header(HttpHeaders.ACCEPT, "application/zip")
                .with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataqueryCrtdlCsv_failsOnJsonError() throws Exception {
        long dataqueryId = 1;

        doReturn(createValidApiDataqueryToGet(dataqueryId)).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));
        doThrow(JsonProcessingException.class).when(dataqueryHandler).createCsvExportZipfile(any(Dataquery.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/" + dataqueryId + "/crtdl"))
                .header(HttpHeaders.ACCEPT, "application/zip")
                .with(csrf()))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataqueryCrtdlCsv_failsOnIoException() throws Exception {
        long dataqueryId = 1;

        doReturn(createValidApiDataqueryToGet(dataqueryId)).when(dataqueryHandler).getDataqueryById(any(Long.class), any(Authentication.class));
        doThrow(IOException.class).when(dataqueryHandler).createCsvExportZipfile(any(Dataquery.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/" + dataqueryId + "/crtdl"))
                .header(HttpHeaders.ACCEPT, "application/zip")
                .with(csrf()))
            .andExpect(status().isInternalServerError());
    }

    @ParameterizedTest
    @CsvSource({"true","false"})
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataqueryList_succeeds(String skipValidation) throws Exception {
        int listSize = 5;
        doReturn(createValidApiDataqueryListToGet(listSize)).when(dataqueryHandler).getDataqueriesByAuthor(any(String.class), anyBoolean());

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "?skip-validation=" + skipValidation)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(listSize))
                .andExpect(jsonPath("$.[0].id").exists());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataqueryList_500onDataqueryException() throws Exception {
        doThrow(DataqueryException.class).when(dataqueryHandler).getDataqueriesByAuthor(any(String.class), anyBoolean());

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "?skip-validation=true")).with(csrf()))
            .andExpect(status().isInternalServerError());
    }

    @ParameterizedTest
    @CsvSource({"true","false"})
    @WithMockUser(roles = "DATAPORTAL_TEST_ADMIN")
    public void testGetDataqueryListByUser_succeeds(String skipValidation) throws Exception {
        int listSize = 5;
        doReturn(createValidApiDataqueryListToGet(listSize)).when(dataqueryHandler).getDataqueriesByAuthor(any(String.class), anyBoolean());

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/by-user/123" + "?skip-validation=" + skipValidation)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(listSize))
            .andExpect(jsonPath("$.[0].id").exists());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_ADMIN")
    public void testGetDataqueryListByUser_500onDataqueryException() throws Exception {
        doThrow(DataqueryException.class).when(dataqueryHandler).getDataqueriesByAuthor(any(String.class), anyBoolean());

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/by-user/123" + "?skip-validation=true")).with(csrf()))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testUpdateDataquery_succeeds() throws Exception {
        doNothing().when(dataqueryHandler).updateDataquery(any(Long.class), any(Dataquery.class), any(String.class));
        doReturn(createSavedQuerySlots()).when(dataqueryHandler).getDataquerySlotsJson(any(String.class));

        mockMvc.perform(put(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/1")).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(createValidDataqueryToStore(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.used").exists())
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testUpdateDataquery_failsOnNotFound() throws Exception {
        doThrow(DataqueryException.class).when(dataqueryHandler).updateDataquery(any(Long.class), any(Dataquery.class), any(String.class));
        mockMvc.perform(put(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/1")).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(createValidDataqueryToStore(1L))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testUpdateDataquery_failsOnJsonProcessingException() throws Exception {
        doThrow(JsonProcessingException.class).when(dataqueryHandler).updateDataquery(any(Long.class), any(Dataquery.class), any(String.class));
        mockMvc.perform(put(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/1")).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(createValidDataqueryToStore(1L))))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testUpdateDataquery_failsOnStorageFull() throws Exception {
        doThrow(DataqueryStorageFullException.class).when(dataqueryHandler).updateDataquery(any(Long.class), any(Dataquery.class), any(String.class));
        mockMvc.perform(put(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/1")).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(createValidDataqueryToStore(1L))))
            .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testDeleteDataquery_succeeds() throws Exception {
        doNothing().when(dataqueryHandler).deleteDataquery(any(Long.class), any(String.class));

        mockMvc.perform(delete(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/1")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testDeleteDataquery_failsWith404OnNotFound() throws Exception {
        doThrow(DataqueryException.class).when(dataqueryHandler).deleteDataquery(any(Long.class), any(String.class));

        mockMvc.perform(delete(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/1")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetDataquerySlots_succeeds() throws Exception {
        doReturn(createSavedQuerySlots()).when(dataqueryHandler).getDataquerySlotsJson(any(String.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_DATA + "/query-slots")).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.used").exists())
            .andExpect(jsonPath("$.total").exists());
    }

    @NotNull
    private Dataquery createValidDataqueryToStore(long id) {
        return Dataquery.builder()
                .id(id)
                .content(createCrtdl())
                .label("TestLabel")
                .comment("TestComment")
                .build();
    }

    @NotNull
    private de.numcodex.feasibility_gui_backend.query.persistence.Dataquery createValidPersistenceDataqueryToGet(long id) throws JsonProcessingException {
        var dataquery = new de.numcodex.feasibility_gui_backend.query.persistence.Dataquery();
        dataquery.setId(id);
        dataquery.setCrtdl(jsonUtil.writeValueAsString(createCrtdl()));
        dataquery.setLabel("TestLabel");
        dataquery.setComment("TestComment");
        dataquery.setLastModified(new Timestamp(new java.util.Date().getTime()));
        return dataquery;
    }

    @NotNull
    private List<de.numcodex.feasibility_gui_backend.query.persistence.Dataquery> createValidPersistenceDataqueryListToGet(int entries) throws JsonProcessingException {
        var dataqueryList = new ArrayList<de.numcodex.feasibility_gui_backend.query.persistence.Dataquery>();
        for (int i = 0; i < entries; ++i) {
            dataqueryList.add(createValidPersistenceDataqueryToGet(i));
        }
        return dataqueryList;
    }

    @NotNull
    private Dataquery createValidApiDataqueryToGet(long id) {
        return Dataquery.builder()
                .id(id)
                .content(createCrtdl())
                .label("TestLabel")
                .comment("TestComment")
                .lastModified(new Timestamp(new Date().getTime()))
                .createdBy("someone")
                .ccdl(CrtdlSectionInfo.builder()
                    .isValid(true)
                    .exists(true)
                    .build())
                .dataExtraction(CrtdlSectionInfo.builder()
                    .isValid(true)
                    .exists(true)
                    .build())
                .build();
    }

    @NotNull
    private List<Dataquery> createValidApiDataqueryListToGet(int entries) throws JsonProcessingException {
        var dataqueryList = new ArrayList<Dataquery>();
        for (int i = 0; i < entries; ++i) {
            dataqueryList.add(createValidApiDataqueryToGet(i));
        }
        return dataqueryList;
    }

    @NotNull
    private Crtdl createCrtdl() {
        return Crtdl.builder()
            .cohortDefinition(createValidStructuredQuery())
            .display("foo")
            .build();
    }

    @NotNull
    private StructuredQuery createValidStructuredQuery() {
        var inclusionCriterion = Criterion.builder()
                .termCodes(List.of(createTermCode()))
                .attributeFilters(List.of())
                .context(createTermCode())
                .build();
        return StructuredQuery.builder()
                .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
                .inclusionCriteria(List.of(List.of(inclusionCriterion)))
                .exclusionCriteria(List.of())
                .display("foo")
                .build();
    }

    @NotNull
    private StructuredQuery createValidAnnotatedStructuredQuery(boolean withIssues) {
        var termCode = TermCode.builder()
            .code("LL2191-6")
            .system("http://loinc.org")
            .display("Geschlecht")
            .build();
        var inclusionCriterion = Criterion.builder()
            .termCodes(List.of(termCode))
            .attributeFilters(List.of())
            .context(termCode)
            .validationIssues(withIssues ? List.of(ValidationIssue.TERMCODE_CONTEXT_COMBINATION_INVALID) : List.of())
            .build();
        return StructuredQuery.builder()
            .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
            .inclusionCriteria(List.of(List.of(inclusionCriterion)))
            .exclusionCriteria(List.of())
            .display("foo")
            .build();
    }

    @NotNull
    private TermCode createTermCode() {
        return TermCode.builder()
                .code("LL2191-6")
                .system("http://loinc.org")
                .display("Geschlecht")
                .build();
    }

    @NotNull
    private Criterion createInvalidCriterion() {
        return Criterion.builder()
            .context(null)
            .termCodes(List.of(createTermCode()))
            .build();
    }

    private SavedQuerySlots createSavedQuerySlots() {
        return SavedQuerySlots.builder()
            .used(5)
            .total(10)
            .build();
    }

    private ByteArrayOutputStream createValidByteArrayOutputStream() throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
        Map<String, String> files = new HashMap<>();
        files.put("foo.json", "{}");
        for (Map.Entry<String, String> file : files.entrySet()) {
            ZipEntry entry = new ZipEntry(file.getKey());
            zipOutputStream.putNextEntry(entry);
            zipOutputStream.write(file.getValue().getBytes());
            zipOutputStream.closeEntry();
        }
        zipOutputStream.close();
        byteArrayOutputStream.close();
        return byteArrayOutputStream;
    }
}
