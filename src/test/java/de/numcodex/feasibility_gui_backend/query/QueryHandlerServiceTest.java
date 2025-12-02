package de.numcodex.feasibility_gui_backend.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.path.NodePath;
import com.networknt.schema.path.PathType;
import de.numcodex.feasibility_gui_backend.query.api.Ccdl;
import de.numcodex.feasibility_gui_backend.query.api.validation.JsonSchemaValidator;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatcher;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryContentRepository;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryRepository;
import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslator;
import de.numcodex.feasibility_gui_backend.terminology.validation.CcdlValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class QueryHandlerServiceTest {


  @Spy
  private ObjectMapper jsonUtil = new ObjectMapper();

  @Mock
  private QueryDispatcher queryDispatcher;

  @Mock
  private QueryRepository queryRepository;

  @Mock
  private QueryContentRepository queryContentRepository;

  @Mock
  private ResultService resultService;

  @Mock
  private CcdlValidation ccdlValidation;

  @Mock
  private JsonSchemaValidator jsonSchemaValidator;

  @Mock
  private QueryTranslator queryTranslator;

  private QueryHandlerService queryHandlerService;

  private QueryHandlerService createQueryHandlerService() {
    return new QueryHandlerService(queryDispatcher, queryRepository, queryContentRepository,
        resultService, ccdlValidation, queryTranslator, jsonSchemaValidator,jsonUtil);
  }

  @BeforeEach
  void setUp() {
    Mockito.reset(queryDispatcher, queryRepository, queryContentRepository,
        resultService, jsonUtil);
    queryHandlerService = createQueryHandlerService();
  }

  @Test
  public void testRunQuery_failsWithMonoErrorOnQueryDispatchException() throws QueryDispatchException {
    var testCcdl = Ccdl.builder()
        .inclusionCriteria(List.of(List.of()))
        .exclusionCriteria(List.of(List.of()))
        .build();
    var queryHandlerService = createQueryHandlerService();
    doThrow(QueryDispatchException.class).when(queryDispatcher).enqueueNewQuery(any(Ccdl.class), any(String.class));

    StepVerifier.create(queryHandlerService.runQuery(testCcdl, "uerid"))
        .expectError(QueryDispatchException.class)
        .verify();
  }

  @Test
  public void testValidateCcdl_noErrors() throws JsonProcessingException {
    JsonNode jsonNode = jsonUtil.readTree("{\"foo\":\"bar\"}");
    doReturn(List.of()).when(jsonSchemaValidator).validate(any(String.class), any(JsonNode.class));

    var errors = queryHandlerService.validateCcdl(jsonNode);

    assertThat(errors).isEmpty();
  }

  @Test
  public void testValidateCcdl_errors() throws JsonProcessingException {
    JsonNode jsonNode = jsonUtil.readTree("{\"foo\":\"bar\"}");
    doReturn(List.of(Error.builder().message("error").instanceLocation(new NodePath(PathType.DEFAULT)).build())).when(jsonSchemaValidator).validate(any(String.class), any(JsonNode.class));

    var errors = queryHandlerService.validateCcdl(jsonNode);

    assertThat(errors).isNotEmpty();
    assertThat(errors.size()).isEqualTo(1);
  }

  @Test
  public void testCcdlFromJsonNode_succeeds() throws Exception {
    JsonNode jsonNode = loadJson("api/validation/ccdl-valid.json");

    var result = assertDoesNotThrow(() -> queryHandlerService.ccdlFromJsonNode(jsonNode));
    assertThat(result).isInstanceOf(Ccdl.class);
  }

  @Test
  public void testCcdlFromJsonNode_throwsOnInvalidJson() throws Exception {
    JsonNode jsonNode = loadJson("api/validation/ccdl-invalid.json");

    assertThrows(IllegalArgumentException.class, () -> queryHandlerService.ccdlFromJsonNode(jsonNode));
  }

  private JsonNode loadJson(String resourcePath) throws Exception {
    try (InputStream is = QueryHandlerServiceTest.class.getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("Resource not found: " + resourcePath);
      }
      return jsonUtil.readTree(is);
    }
  }
}
