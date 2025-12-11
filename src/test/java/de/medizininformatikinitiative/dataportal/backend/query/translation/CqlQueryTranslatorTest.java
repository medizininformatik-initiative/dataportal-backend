package de.medizininformatikinitiative.dataportal.backend.query.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.medizininformatikinitiative.dataportal.backend.common.api.Criterion;
import de.medizininformatikinitiative.dataportal.backend.common.api.TermCode;
import de.medizininformatikinitiative.dataportal.backend.query.api.Ccdl;
import de.numcodex.sq2cql.Translator;
import de.numcodex.sq2cql.model.cql.Container;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Tag("query")
@Tag("translation")
@ExtendWith(MockitoExtension.class)
public class CqlQueryTranslatorTest {
  @Mock
  private Translator translator;

  @Spy
  private ObjectMapper jsonUtil;

  @InjectMocks
  private CqlQueryTranslator cqlQueryTranslator;

  @Test
  public void testTranslate_ModelConversionFailsDuringEncoding() throws JsonProcessingException {
    var testQuery = Ccdl.builder().build();
    doThrow(JsonProcessingException.class).when(jsonUtil).writeValueAsString(testQuery);

    assertThrows(QueryTranslationException.class, () -> cqlQueryTranslator.translate(testQuery));
    verify(jsonUtil, never()).readValue(anyString(), eq(de.numcodex.sq2cql.model.structured_query.StructuredQuery.class));
    verifyNoInteractions(translator);
  }

  @Test
  public void testTranslate_ModelConversionFailsDuringDecoding() throws JsonProcessingException {
    var testQuery = Ccdl.builder().build();
    doReturn("foo").when(jsonUtil).writeValueAsString(testQuery);
    doThrow(JsonProcessingException.class).when(jsonUtil).readValue("foo",
        de.numcodex.sq2cql.model.structured_query.StructuredQuery.class);

    assertThrows(QueryTranslationException.class, () -> cqlQueryTranslator.translate(testQuery));
    verify(jsonUtil).readValue("foo", de.numcodex.sq2cql.model.structured_query.StructuredQuery.class);
    verifyNoInteractions(translator);
  }

  @Disabled("Needs to be enabled if the new version of sq2cl is available and compatible with structured query v2.")
  @Test
  public void testTranslate_TranslationFails() {
    var termCode = TermCode.builder()
        .code("LL2191-6")
        .system("http://loinc.org")
        .display("Geschlecht")
        .build();
    var inclusionCriterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .build();
    var testQuery = Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(inclusionCriterion)))
        .exclusionCriteria(List.of(List.of()))
        .display("foo")
        .build();

    doThrow(NullPointerException.class).when(translator)
        .toCql(any(de.numcodex.sq2cql.model.structured_query.StructuredQuery.class));

    assertThrows(QueryTranslationException.class, () -> cqlQueryTranslator.translate(testQuery));
    verify(translator).toCql(any());
  }

  @Disabled("Needs to be enabled if the new version of sq2cl is available and compatible with structured query v2.")
  @Test
  public void testTranslate_EverythingSucceeds() throws QueryTranslationException {
    var termCode = TermCode.builder()
        .code("LL2191-6")
        .system("http://loinc.org")
        .display("Geschlecht")
        .build();
    var inclusionCriterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .build();
    var testQuery = Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(inclusionCriterion)))
        .exclusionCriteria(List.of(List.of()))
        .display("foo")
        .build();

    var resultLibraryMock = mock(Container.class);
    when(resultLibraryMock.print()).thenReturn("bar");
    when(translator.toCql(any(de.numcodex.sq2cql.model.structured_query.StructuredQuery.class)))
        .thenReturn(resultLibraryMock);

    var translationResult = cqlQueryTranslator.translate(testQuery);
    assertEquals("bar", translationResult);

  }
}
