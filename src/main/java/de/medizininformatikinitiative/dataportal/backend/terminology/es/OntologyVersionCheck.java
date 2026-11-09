package de.medizininformatikinitiative.dataportal.backend.terminology.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import de.medizininformatikinitiative.dataportal.backend.config.PropertiesReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.medizininformatikinitiative.dataportal.backend.terminology.OntologyInfoContributor.KEY_ONTOLOGY_TAG_PROPERTIES;

/**
 * Compares the ontology version this backend was built for with the {@code _meta.version} of the Elasticsearch indices.
 * <p>
 * Runs once on startup, before the web server accepts requests. Depending on {@link Mode}, a failed check either
 * aborts the startup or logs a warning.
 */
@Component
@ConditionalOnExpression("${app.elastic.enabled}")
public class OntologyVersionCheck implements SmartInitializingSingleton {

  public enum Mode {STRICT, WARN, OFF}

  private static final Logger log = LoggerFactory.getLogger(OntologyVersionCheck.class);

  private static final List<String> INDICES = List.of("ontology", "codeable_concept", "profile");
  private static final String META_VERSION_KEY = "version";
  private static final String CONFIG_HINT = "This check is controlled by ELASTIC_SEARCH_ONTOLOGY_VERSION_CHECK (strict|warn|off).";
  private static final int DEFAULT_MAX_ATTEMPTS = 6;
  private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(5);

  private final ElasticsearchClient client;
  private final PropertiesReader propertiesReader;
  private final Mode mode;
  private final int maxAttempts;
  private final Duration retryDelay;

  @Autowired
  public OntologyVersionCheck(ElasticsearchClient client,
                              PropertiesReader propertiesReader,
                              @Value("${app.elastic.ontologyVersionCheck:warn}") Mode mode) {
    this(client, propertiesReader, mode, DEFAULT_MAX_ATTEMPTS, DEFAULT_RETRY_DELAY);
  }

  OntologyVersionCheck(ElasticsearchClient client, PropertiesReader propertiesReader, Mode mode, int maxAttempts,
                       Duration retryDelay) {
    this.client = client;
    this.propertiesReader = propertiesReader;
    this.mode = mode;
    this.maxAttempts = maxAttempts;
    this.retryDelay = retryDelay;
  }

  @Override
  public void afterSingletonsInstantiated() {
    if (mode == Mode.OFF) {
      log.info("Elasticsearch ontology version check is disabled.");
      return;
    }

    var expected = propertiesReader.getValue(KEY_ONTOLOGY_TAG_PROPERTIES);
    if (expected == null || expected.isBlank()) {
      report("""
          Could not verify the ontology version of Elasticsearch: this build does not state which ontology version \
          it expects. If the ontology in Elasticsearch does not match this backend, errors may occur, for example \
          empty or wrong search results, or failing requests.""");
      return;
    }

    try {
      var problems = findProblemsWithRetry(expected);
      if (problems.isEmpty()) {
        log.info("Elasticsearch indices {} match the expected ontology version {}.", INDICES, expected);
      } else {
        report("""
            Ontology version mismatch: this backend expects ontology %s, but Elasticsearch does not match: %s. \
            This may cause errors, for example empty or wrong search results, or failing requests. \
            To fix this, load ontology %s into Elasticsearch using dataportal-es-init, or use a backend version \
            that matches the ontology in Elasticsearch."""
            .formatted(expected, String.join("; ", problems), expected));
      }
    } catch (EsUnavailableException e) {
      report("""
          Could not verify the ontology version of Elasticsearch after %d attempts: %s. Elasticsearch may be \
          unavailable. If its ontology does not match %s, errors may occur, for example empty or wrong search \
          results, or failing requests."""
          .formatted(maxAttempts, e.getMessage(), expected));
    }
  }

  private List<String> findProblemsWithRetry(String expected) throws EsUnavailableException {
    Exception lastFailure = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        return findProblems(expected);
      } catch (IOException | RuntimeException e) {
        // Also covers responses that can't be parsed: in warn mode, those must not prevent the startup either.
        lastFailure = e;
        log.debug("Attempt {}/{} to read the ontology version from Elasticsearch failed.", attempt, maxAttempts, e);
      }
      if (attempt < maxAttempts && !sleepBeforeRetry()) {
        break;
      }
    }
    throw new EsUnavailableException(lastFailure);
  }

  private boolean sleepBeforeRetry() {
    try {
      Thread.sleep(retryDelay.toMillis());
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private List<String> findProblems(String expected) throws IOException {
    var problems = new ArrayList<String>();
    for (var index : INDICES) {
      try {
        var version = readVersion(index);
        if (version.isEmpty()) {
          problems.add("index '%s' has no _meta.version".formatted(index));
        } else if (!expected.equals(version.get())) {
          problems.add("index '%s' has version %s".formatted(index, version.get()));
        }
      } catch (ElasticsearchException e) {
        if (e.status() != 404) {
          throw e;
        }
        problems.add("index '%s' does not exist".formatted(index));
      }
    }
    return problems;
  }

  private Optional<String> readVersion(String index) throws IOException {
    return client.indices().getMapping(GetMappingRequest.of(r -> r.index(index))).mappings().values().stream()
        .map(record -> record.mappings().meta().get(META_VERSION_KEY))
        .filter(Objects::nonNull)
        .map(version -> version.to(String.class))
        .findFirst();
  }

  private void report(String message) {
    if (mode == Mode.STRICT) {
      throw new IllegalStateException(message + " Aborting startup. " + CONFIG_HINT);
    }
    log.warn("{} Continuing startup anyway. {}", message, CONFIG_HINT);
  }

  private static final class EsUnavailableException extends Exception {
    EsUnavailableException(Exception cause) {
      super(String.valueOf(cause), cause);
    }
  }
}
