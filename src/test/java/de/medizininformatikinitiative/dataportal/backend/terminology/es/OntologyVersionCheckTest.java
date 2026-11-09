package de.medizininformatikinitiative.dataportal.backend.terminology.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.json.JsonData;
import de.medizininformatikinitiative.dataportal.backend.config.PropertiesReader;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.OntologyVersionCheck.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.time.Duration;

import static de.medizininformatikinitiative.dataportal.backend.terminology.OntologyInfoContributor.KEY_ONTOLOGY_TAG_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@Tag("terminology")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OntologyVersionCheckTest {

  private static final String EXPECTED = "v5.1.0";
  private static final int MAX_ATTEMPTS = 3;

  @Mock
  private ElasticsearchClient client;
  @Mock
  private ElasticsearchIndicesClient indicesClient;
  @Mock
  private PropertiesReader propertiesReader;

  @BeforeEach
  void setUp() {
    doReturn(indicesClient).when(client).indices();
    doReturn(EXPECTED).when(propertiesReader).getValue(KEY_ONTOLOGY_TAG_PROPERTIES);
  }

  private OntologyVersionCheck check(Mode mode) {
    return new OntologyVersionCheck(client, propertiesReader, mode, MAX_ATTEMPTS, Duration.ZERO);
  }

  private void givenIndexVersion(String index, String version) throws IOException {
    var response = GetMappingResponse.of(r -> r.mappings(index, m -> m.mappings(t -> t.meta("version", JsonData.of(version)))));
    doReturn(response).when(indicesClient).getMapping(argThat((GetMappingRequest r) -> r != null && r.index().contains(index)));
  }

  private void givenAllIndicesHaveVersion(String version) throws IOException {
    givenIndexVersion("ontology", version);
    givenIndexVersion("codeable_concept", version);
    givenIndexVersion("profile", version);
  }

  private static ElasticsearchException esException(int status) {
    return new ElasticsearchException("test", ErrorResponse.of(e -> e.status(status).error(c -> c.reason("test"))));
  }

  @Test
  void strict_startsWhenAllIndicesMatch() throws IOException {
    givenAllIndicesHaveVersion(EXPECTED);

    assertThatCode(() -> check(Mode.STRICT).afterSingletonsInstantiated()).doesNotThrowAnyException();
  }

  @Test
  void strict_failsAndNamesTheMismatchingIndexAndTheRiskOfErrors() throws IOException {
    givenAllIndicesHaveVersion(EXPECTED);
    givenIndexVersion("profile", "v5.0.0");

    assertThatThrownBy(() -> check(Mode.STRICT).afterSingletonsInstantiated())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("expects ontology v5.1.0")
        .hasMessageContaining("index 'profile' has version v5.0.0")
        .hasMessageNotContaining("index 'ontology'")
        .hasMessageContaining("may cause errors")
        .hasMessageContaining("Aborting startup")
        .hasMessageContaining("ELASTIC_SEARCH_ONTOLOGY_VERSION_CHECK_MODE");
  }

  @Test
  void strict_failsWhenVersionMarkerIsMissing() throws IOException {
    givenAllIndicesHaveVersion(EXPECTED);
    doReturn(GetMappingResponse.of(r -> r.mappings("ontology", m -> m.mappings(t -> t))))
        .when(indicesClient).getMapping(argThat((GetMappingRequest r) -> r != null && r.index().contains("ontology")));

    assertThatThrownBy(() -> check(Mode.STRICT).afterSingletonsInstantiated())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("index 'ontology' has no _meta.version");
  }

  @Test
  void strict_failsWithoutRetryWhenIndexDoesNotExist() throws IOException {
    doThrow(esException(404)).when(indicesClient).getMapping(any(GetMappingRequest.class));

    assertThatThrownBy(() -> check(Mode.STRICT).afterSingletonsInstantiated())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("index 'ontology' does not exist")
        .hasMessageContaining("index 'codeable_concept' does not exist")
        .hasMessageContaining("index 'profile' does not exist");

    // one request per index, so a single pass
    verify(indicesClient, times(3)).getMapping(any(GetMappingRequest.class));
  }

  @Test
  void strict_failsAfterRetriesWhenElasticsearchIsUnreachable() throws IOException {
    doThrow(new IOException("Connection refused")).when(indicesClient).getMapping(any(GetMappingRequest.class));

    assertThatThrownBy(() -> check(Mode.STRICT).afterSingletonsInstantiated())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Could not verify")
        .hasMessageContaining("Connection refused")
        .hasMessageContaining("errors may occur");

    verify(indicesClient, times(MAX_ATTEMPTS)).getMapping(any(GetMappingRequest.class));
  }

  @Test
  void strict_retriesOnServerErrorAndSucceedsWhenElasticsearchRecovers() throws IOException {
    givenAllIndicesHaveVersion(EXPECTED);
    var healthy = indicesClient.getMapping(GetMappingRequest.of(r -> r.index("ontology")));
    clearInvocations(indicesClient);
    doThrow(esException(503)).doReturn(healthy)
        .when(indicesClient).getMapping(argThat((GetMappingRequest r) -> r != null && r.index().contains("ontology")));

    assertThatCode(() -> check(Mode.STRICT).afterSingletonsInstantiated()).doesNotThrowAnyException();

    verify(indicesClient, times(2)).getMapping(argThat((GetMappingRequest r) -> r != null && r.index().contains("ontology")));
  }

  @Test
  void strict_stopsRetryingWhenInterrupted() throws IOException {
    doThrow(new IOException("Connection refused")).when(indicesClient).getMapping(any(GetMappingRequest.class));
    var check = new OntologyVersionCheck(client, propertiesReader, Mode.STRICT, MAX_ATTEMPTS, Duration.ofHours(1));

    Thread.currentThread().interrupt();
    try {
      assertThatThrownBy(check::afterSingletonsInstantiated)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Could not verify")
          .hasMessageContaining("Connection refused");

      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      // clear the flag, so that it doesn't leak into other tests
      Thread.interrupted();
    }

    verify(indicesClient).getMapping(any(GetMappingRequest.class));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " "})
  void strict_failsWhenExpectedVersionIsUnknown(String expected) {
    doReturn(expected).when(propertiesReader).getValue(KEY_ONTOLOGY_TAG_PROPERTIES);

    assertThatThrownBy(() -> check(Mode.STRICT).afterSingletonsInstantiated())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("does not state which ontology version");

    verifyNoInteractions(indicesClient);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " "})
  void warn_doesNotFailWhenExpectedVersionIsUnknown(String expected) {
    doReturn(expected).when(propertiesReader).getValue(KEY_ONTOLOGY_TAG_PROPERTIES);

    assertThatCode(() -> check(Mode.WARN).afterSingletonsInstantiated()).doesNotThrowAnyException();

    verifyNoInteractions(indicesClient);
  }

  @Test
  void warn_doesNotFailOnMismatch() throws IOException {
    givenAllIndicesHaveVersion("v5.0.0");

    assertThatCode(() -> check(Mode.WARN).afterSingletonsInstantiated()).doesNotThrowAnyException();
  }

  @Test
  void warn_doesNotFailWhenElasticsearchIsUnreachable() throws IOException {
    doThrow(new IOException("Connection refused")).when(indicesClient).getMapping(any(GetMappingRequest.class));

    assertThatCode(() -> check(Mode.WARN).afterSingletonsInstantiated()).doesNotThrowAnyException();
  }

  @Test
  void off_doesNotContactElasticsearch() {
    check(Mode.OFF).afterSingletonsInstantiated();

    verifyNoInteractions(client);
  }

  @Test
  void strict_abortsApplicationContextStartupOnMismatch() throws IOException {
    givenAllIndicesHaveVersion("v5.0.0");

    new ApplicationContextRunner()
        .withPropertyValues("app.elastic.enabled=true")
        .withBean(OntologyVersionCheck.class, () -> check(Mode.STRICT))
        .run(context -> assertThat(context).getFailure()
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ontology version mismatch"));
  }

  @Test
  void isNotActiveWhenElasticsearchIsDisabled() {
    new ApplicationContextRunner()
        .withPropertyValues("app.elastic.enabled=false", "app.elastic.ontologyVersionCheck.mode=strict")
        .withBean(ElasticsearchClient.class, () -> client)
        .withBean(PropertiesReader.class, () -> propertiesReader)
        .withUserConfiguration(OntologyVersionCheck.class)
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).doesNotHaveBean(OntologyVersionCheck.class);
        });

    verifyNoInteractions(indicesClient);
  }

  private ApplicationContextRunner contextRunnerWithCheck() {
    return new ApplicationContextRunner()
        .withPropertyValues("app.elastic.enabled=true", "app.elastic.ontologyVersionCheck.mode=WARN")
        .withBean(ElasticsearchClient.class, () -> client)
        .withBean(PropertiesReader.class, () -> propertiesReader)
        .withUserConfiguration(OntologyVersionCheck.class);
  }

  @Test
  void usesTheConfiguredAttemptsAndInterval() throws IOException {
    doThrow(new IOException("Connection refused")).when(indicesClient).getMapping(any(GetMappingRequest.class));

    contextRunnerWithCheck()
        .withPropertyValues("app.elastic.ontologyVersionCheck.attempts=2", "app.elastic.ontologyVersionCheck.interval=PT0S")
        .run(context -> assertThat(context).hasNotFailed());

    verify(indicesClient, times(2)).getMapping(any(GetMappingRequest.class));
  }

  @Test
  void intervalMustBeAnIsoDuration() {
    contextRunnerWithCheck()
        .withPropertyValues("app.elastic.ontologyVersionCheck.interval=10s")
        .run(context -> assertThat(context).hasFailed());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void attemptsMustBeAtLeastOne(int attempts) {
    assertThatThrownBy(() -> new OntologyVersionCheck(client, propertiesReader, Mode.WARN, attempts, Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("app.elastic.ontologyVersionCheck.attempts");
  }

  @Test
  void intervalMustNotBeNegative() {
    assertThatThrownBy(() -> new OntologyVersionCheck(client, propertiesReader, Mode.WARN, 1, Duration.ofSeconds(-1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("app.elastic.ontologyVersionCheck.interval");
  }
}
