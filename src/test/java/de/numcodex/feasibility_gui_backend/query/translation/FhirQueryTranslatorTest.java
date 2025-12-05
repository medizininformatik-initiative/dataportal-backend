package de.numcodex.feasibility_gui_backend.query.translation;


import com.fasterxml.jackson.core.JsonProcessingException;
import tools.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("query")
@Tag("translation")
@ExtendWith(MockitoExtension.class)
public class FhirQueryTranslatorTest {

    @Mock
    private RestTemplate client;

    @Spy
    private ObjectMapper jsonUtil;

    @InjectMocks
    private FhirQueryTranslator fhirQueryTranslator;

    @Captor
    private ArgumentCaptor<HttpEntity<String>> requestCaptor;

    @Test
    public void testTranslate_EncodingStructuredQueryForRequestFails() throws JsonProcessingException {
        var testQuery = StructuredQuery.builder().build();
        doThrow(JsonProcessingException.class).when(jsonUtil).writeValueAsString(testQuery);

        assertThrows(QueryTranslationException.class, () -> fhirQueryTranslator.translate(testQuery));
        verify(jsonUtil).writeValueAsString(testQuery);
        verifyNoInteractions(client);
    }

    @Test
    public void testTranslate_RequestToExternalTranslationServiceFails() throws JsonProcessingException {
        var testQuery = StructuredQuery.builder().build();
        doReturn("foo").when(jsonUtil).writeValueAsString(testQuery);
        doThrow(RestClientException.class).when(client)
                .postForObject(anyString(), any(), eq(String.class));

        assertThrows(QueryTranslationException.class, () -> fhirQueryTranslator.translate(testQuery));
        verify(jsonUtil).writeValueAsString(testQuery);
        verify(client).postForObject(anyString(), any(), eq(String.class));
    }

    @Test
    public void testTranslate_EverythingSucceeds() throws JsonProcessingException, QueryTranslationException {
        var testQuery = StructuredQuery.builder().build();
        doReturn("foo").when(jsonUtil).writeValueAsString(testQuery);
        when(client.postForObject(eq("/query/translate"), requestCaptor.capture(), eq(String.class)))
                .thenReturn("bar");

        var translationResult = fhirQueryTranslator.translate(testQuery);
        assertEquals("bar", translationResult);
        assertEquals("foo", requestCaptor.getValue().getBody());
        assertEquals("CSQ", requestCaptor.getValue().getHeaders().getFirst(HttpHeaders.ACCEPT_ENCODING));
        assertNotNull(requestCaptor.getValue().getHeaders().getContentType());
        assertEquals("application/json", requestCaptor.getValue().getHeaders().getContentType().toString());
    }
}
