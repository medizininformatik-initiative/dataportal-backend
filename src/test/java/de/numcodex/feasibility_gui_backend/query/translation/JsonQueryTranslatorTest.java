package de.numcodex.feasibility_gui_backend.query.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.Ccdl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

@Tag("query")
@Tag("translation")
@ExtendWith(MockitoExtension.class)
public class JsonQueryTranslatorTest {

    @Spy
    private ObjectMapper jsonUtil;

    @InjectMocks
    private JsonQueryTranslator jsonQueryTranslator;

    @Test
    public void testTranslate_TranslationFails() throws JsonProcessingException {
        var testQuery = Ccdl.builder().build();
        doThrow(JsonProcessingException.class).when(jsonUtil).writeValueAsString(testQuery);

        assertThrows(QueryTranslationException.class, () -> jsonQueryTranslator.translate(testQuery));
    }

    @Test
    public void testTranslate_TranslationSucceeds() {
        var testQuery = Ccdl.builder().build();
        assertDoesNotThrow(() -> jsonQueryTranslator.translate(testQuery));
    }
}
