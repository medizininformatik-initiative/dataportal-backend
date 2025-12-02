package de.fdpg.dataportal_backend.query.broker.direct;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.google.common.base.Charsets;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.netty.handler.timeout.ReadTimeoutException;
import okhttp3.Headers;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.from;

@Testcontainers
@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
public class DirectSpringConfigIT {

    public static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.2")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .withRealmImportFile(new ClassPathResource("realm-test.json", DirectSpringConfigIT.class).getPath())
            .withReuse(true);

    private static final String USERNAME = "some-user-123";
    private static final String PASSWORD = "vALBAi95WW84x3";
    MockWebServer mockWebServer;

    private DirectSpringConfig directSpringConfig;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Direct broker FLARE webclient request with basic authentication")
    void flareClientWithCredentials() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("Foo"));
        directSpringConfig = new DirectSpringConfig(true, String.format("http://localhost:%s", mockWebServer.getPort()),
                null, USERNAME, PASSWORD, null, null, null, Duration.ofSeconds(10), false);
        var authHeaderValue = "Basic "
                + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

        WebClient webClient = directSpringConfig.directWebClientFlare();

        webClient
                .get()
                .uri("/foo")
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(responseBody -> {});
        var recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo(authHeaderValue);
    }

    @Test
    @DisplayName("Direct broker FLARE webclient request with no authentication")
    void flareClientWithoutCredentials() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("Foo"));
        directSpringConfig = new DirectSpringConfig(true, String.format("http://localhost:%s", mockWebServer.getPort()),
                null, null, null, null, null, null, Duration.ofSeconds(10), false);

        WebClient webClient = directSpringConfig.directWebClientFlare();

        webClient
                .get()
                .uri("/foo")
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(responseBody -> {});
        var recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    @DisplayName("Direct broker FHIR client request with OAuth token")
    void fhirClientWithOAuthCredentials() throws InterruptedException, IOException {
        keycloak.start();
        try {
            String metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
                    .getContentAsString(Charsets.UTF_8);
            mockWebServer.setDispatcher(new Dispatcher() {

                @Override
                public MockResponse dispatch(RecordedRequest arg0) throws InterruptedException {
                    if ("/metadata".equals(arg0.getPath())) {
                        return new MockResponse().setResponseCode(200)
                                .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
                                .setBody(metadata);
                    } else {
                        return new MockResponse().setResponseCode(404);
                    }
                    }
            });
            directSpringConfig = new DirectSpringConfig(true, null,
                    String.format("http://localhost:%s", mockWebServer.getPort()), null, null,
                    String.format("http://localhost:%s/realms/test", keycloak.getFirstMappedPort()), "account", "test",
                    Duration.ofSeconds(10), false);
            IGenericClient client = directSpringConfig.getFhirClient(FhirContext.forR4());

            client.capabilities().ofType(CapabilityStatement.class).execute();

            var recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).startsWith("Bearer ey");
        } finally {
            keycloak.stop();
        }
    }

    @Test
    @DisplayName("flare webClient succeeds getting 1s delayed response before timeout of 5s")
    void flareClientSucceedsFinishingBeforeTimeout() throws Exception {
        var timeout = Duration.ofSeconds(5);
        var body = "Foo";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(body).setBodyDelay(1, SECONDS));
        directSpringConfig = new DirectSpringConfig(false,
                String.format("http://localhost:%s", mockWebServer.getPort()),
                null, null, null, null, null, null, timeout, false);
        var client = directSpringConfig.directWebClientFlare();

        Instant start = Instant.now();
        var response = client.get()
                .uri("/foo")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertThat(response).isEqualTo(body);
        assertThat(Duration.between(start, Instant.now())).isLessThan(timeout);
    }

    @Test
    @DisplayName("flare webClient fails not getting 5s delayed response before given timeout of 2s")
    void flareClientFailsReachingTimeout() throws Exception {
        var timeout = Duration.ofSeconds(2);
        var delta = Duration.ofSeconds(3);
        var delay = 5;
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("Foo").setBodyDelay(delay, SECONDS));
        directSpringConfig = new DirectSpringConfig(false,
                String.format("http://localhost:%s", mockWebServer.getPort()),
                null, null, null, null, null, null, timeout, false);
        var client = directSpringConfig.directWebClientFlare();

        Instant start = Instant.now();
        assertThatThrownBy(() -> client.get()
                .uri("/foo")
                .retrieve()
                .bodyToMono(String.class)
                .block()).isInstanceOf(WebClientResponseException.class)
                        .hasCauseInstanceOf(ReadTimeoutException.class);
        var elapsed = Duration.between(start, Instant.now());
        assertThat(elapsed).isBetween(timeout, timeout.plus(delta));
        Thread.sleep(Duration.ofSeconds(delay).minus(elapsed).toMillis());
    }

    @Test
    @DisplayName("FHIR client succeeds getting 1s delayed response before given timeout of 5s")
    void fhirClientSucceedsFinishingBeforeTimeout() throws Exception {
        var metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
                .getContentAsString(Charsets.UTF_8);
        var timeout = Duration.ofSeconds(5);
        var delta = Duration.ofSeconds(1);
        var delay = 1;
        var response = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
                .setBody(metadata);
        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response.clone()
                .setBodyDelay(delay, SECONDS));
        directSpringConfig = new DirectSpringConfig(true, null,
                String.format("http://localhost:%s", mockWebServer.getPort()), null, null, null, null, null, timeout,
                false);
        var client = directSpringConfig.getFhirClient(FhirContext.forR4());

        Instant start = Instant.now();
        client.capabilities().ofType(CapabilityStatement.class).execute();

        assertThat(Duration.between(start, Instant.now())).isLessThan(timeout.plus(delta));
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        assertThat(mockWebServer.takeRequest().getPath())
                .isEqualTo(mockWebServer.takeRequest().getPath())
                .isEqualTo("/metadata");
    }

    @Test
    @DisplayName("FHIR client fails not getting 5s delayed response before given timeout of 2s")
    void fhirClientFailsReachingTimeout() throws Exception {
        var metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
                .getContentAsString(Charsets.UTF_8);
        var timeout = Duration.ofSeconds(2);
        var delta = Duration.ofSeconds(3);
        var delay = 5;
        var response = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
                .setBody(metadata);
        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response.clone()
                .setBodyDelay(delay, SECONDS));
        directSpringConfig = new DirectSpringConfig(true, null,
                String.format("http://localhost:%s", mockWebServer.getPort()), null, null, null, null, null, timeout,
                false);
        var client = directSpringConfig.getFhirClient(FhirContext.forR4());

        Instant start = Instant.now();
        assertThatThrownBy(() -> client.capabilities().ofType(CapabilityStatement.class).execute())
                .isInstanceOf(FhirClientConnectionException.class);
        var elapsed = Duration.between(start, Instant.now());
        assertThat(elapsed).isBetween(timeout, timeout.plus(delta));
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        assertThat(mockWebServer.takeRequest().getPath())
                .isEqualTo(mockWebServer.takeRequest().getPath())
                .isEqualTo("/metadata");
        Thread.sleep(Duration.ofSeconds(delay).minus(elapsed).toMillis());
    }

    @Test
    @DisplayName("FHIR client uses Async Interaction Request Pattern successfully")
    void asyncFhirClientSucceeds() throws Exception {
        var metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
                .getContentAsString(Charsets.UTF_8);
        var statusPath = "/Measure/foo/bar/status";
        var body = "bar";
        var metadataResponse = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
                .setBody(metadata);
        var kickOffResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of("Content-Location",
                        "http://localhost:%s%s".formatted(mockWebServer.getPort(), statusPath)))
                .setBody("");
        var inProgressResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of("X-Progress", "0%"))
                .setBody("");
        var finalResponse = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "text/plain"))
                .setBody(body);
        mockWebServer.enqueue(metadataResponse);
        mockWebServer.enqueue(kickOffResponse);
        mockWebServer.enqueue(inProgressResponse);
        mockWebServer.enqueue(inProgressResponse);
        mockWebServer.enqueue(finalResponse);
        directSpringConfig = new DirectSpringConfig(true, null,
                "http://localhost:%s".formatted(mockWebServer.getPort()), null, null, null, null, null,
                Duration.ofSeconds(10), true);
        var client = directSpringConfig.getFhirClient(FhirContext.forR4());

        var result = client.operation().onInstance("Measure/foo").named("bar").withNoParameters(Parameters.class)
                .returnResourceType(Binary.class).execute();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(5);
        assertThat(mockWebServer.takeRequest())
                .returns("/metadata", from(r -> r.getPath()))
                .returns("respond-async", from(r -> r.getHeader("Prefer")));
        assertThat(mockWebServer.takeRequest())
                .matches(r -> "/Measure/foo/$bar".equals(r.getPath()), "path is '/Measure/foo/$bar'");
        assertThat(mockWebServer.takeRequest())
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath));
        assertThat(mockWebServer.takeRequest())
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath));
        assertThat(mockWebServer.takeRequest())
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath));
        assertThat(new String(result.getContent())).isEqualTo(body);
    }

    @Test
    @DisplayName("Async FHIR client fails when kickoff response contains no Content-Location header")
    void asyncFhirClientFailsWithNoContentLocation() throws Exception {
        var metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
                .getContentAsString(Charsets.UTF_8);
        var metadataResponse = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
                .setBody(metadata);
        var kickOffResponse = new MockResponse().setResponseCode(202)
                .setBody("");
        mockWebServer.enqueue(metadataResponse);
        mockWebServer.enqueue(kickOffResponse);
        directSpringConfig = new DirectSpringConfig(true, null,
                "http://localhost:%s".formatted(mockWebServer.getPort()), null, null, null, null, null,
                Duration.ofSeconds(10), true);
        var client = directSpringConfig.getFhirClient(FhirContext.forR4());

        assertThatThrownBy(
                () -> client.operation().onInstance("Measure/foo").named("bar").withNoParameters(Parameters.class)
                        .returnResourceType(Binary.class).execute())
                                .hasCauseInstanceOf(AsyncRequestException.class)
                                .cause().hasMessageContaining("No Content-Location provided for polling");

        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        assertThat(mockWebServer.takeRequest())
                .returns("/metadata", from(r -> r.getPath()))
                .returns("respond-async", from(r -> r.getHeader("Prefer")));
        assertThat(mockWebServer.takeRequest())
                .matches(r -> "/Measure/foo/$bar".equals(r.getPath()), "path is '/Measure/foo/$bar'");
    }

    @Test
    @DisplayName("Async FHIR client respects the Retry-After header of server response")
    void asyncFhirClientRespectsRetryAfterHeader() throws Exception {
        var metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
                .getContentAsString(Charsets.UTF_8);
        var statusPath = "/Measure/foo/bar/status";
        var body = "bar";
        var start = Instant.now();
        var metadataResponse = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
                .setBody(metadata);
        var kickOffResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of("Content-Location",
                        "http://localhost:%s%s".formatted(mockWebServer.getPort(), statusPath)))
                .setBody("");
        // Use HTTP date format as Retry-After header value
        var inProgressResponse1 = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of(HttpHeaders.RETRY_AFTER,
                        DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT"))
                                .withLocale(Locale.US)
                                .format(Instant.now().plus(Duration.ofSeconds(5)))))
                .setBody("");
        // Use integer value as Retra-After header value representing seconds
        var inProgressResponse2 = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of(HttpHeaders.RETRY_AFTER, "3"))
                .setBody("");
        var finalResponse = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "text/plain"))
                .setBody(body);
        mockWebServer.enqueue(metadataResponse);
        mockWebServer.enqueue(kickOffResponse);
        mockWebServer.enqueue(inProgressResponse1);
        mockWebServer.enqueue(inProgressResponse2);
        mockWebServer.enqueue(finalResponse);
        directSpringConfig = new DirectSpringConfig(true, null,
                "http://localhost:%s".formatted(mockWebServer.getPort()), null, null, null, null, null,
                Duration.ofSeconds(20), true);
        var client = directSpringConfig.getFhirClient(FhirContext.forR4());

        var result = client.operation().onInstance("Measure/foo").named("bar").withNoParameters(Parameters.class)
                .returnResourceType(Binary.class).execute();

        assertThat(Duration.between(start, Instant.now()))
                .isGreaterThan(Duration.ofSeconds(5));
        assertThat(mockWebServer.getRequestCount()).isEqualTo(5);
        assertThat(mockWebServer.takeRequest())
                .returns("/metadata", from(r -> r.getPath()))
                .returns("respond-async", from(r -> r.getHeader("Prefer")));
        assertThat(mockWebServer.takeRequest())
                .matches(r -> "/Measure/foo/$bar".equals(r.getPath()), "path is '/Measure/foo/$bar'");
        assertThat(mockWebServer.takeRequest())
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath));
        assertThat(mockWebServer.takeRequest())
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath));
        assertThat(mockWebServer.takeRequest())
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath));
        assertThat(new String(result.getContent())).isEqualTo(body);
    }

    @Test
    @DisplayName("Async FHIR client uses predefined exponential timeout on invalid Retry-After header value")
    void asyncFhirClientIgnoresInvalidRetryAfterHeader(CapturedOutput output) throws Exception {
        var metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
                .getContentAsString(Charsets.UTF_8);
        var statusPath = "/Measure/foo/bar/status";
        var body = "bar";
        var start = Instant.now();
        var metadataResponse = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
                .setBody(metadata);
        var kickOffResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of("Content-Location",
                        "http://localhost:%s%s".formatted(mockWebServer.getPort(), statusPath)))
                .setBody("");
        var inProgressResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of(HttpHeaders.RETRY_AFTER, "five seconds"))
                .setBody("");
        var finalResponse = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "text/plain"))
                .setBody(body);
        mockWebServer.enqueue(metadataResponse);
        mockWebServer.enqueue(kickOffResponse);
        mockWebServer.enqueue(inProgressResponse); // 250ms wait time
        mockWebServer.enqueue(inProgressResponse); // 500ms wait time
        mockWebServer.enqueue(inProgressResponse); // 1000ms wait time
        mockWebServer.enqueue(finalResponse);
        directSpringConfig = new DirectSpringConfig(true, null,
                "http://localhost:%s".formatted(mockWebServer.getPort()), null, null, null, null, null,
                Duration.ofSeconds(20), true);
        var client = directSpringConfig.getFhirClient(FhirContext.forR4());

        var result = client.operation().onInstance("Measure/foo").named("bar").withNoParameters(Parameters.class)
                .returnResourceType(Binary.class).execute();

        assertThat(Duration.between(start, Instant.now()))
                .isGreaterThan(Duration.ofMillis(1750));
        assertThat(mockWebServer.getRequestCount()).isEqualTo(6);
        assertThat(mockWebServer.takeRequest())
                .returns("/metadata", from(r -> r.getPath()))
                .returns("respond-async", from(r -> r.getHeader("Prefer")));
        assertThat(mockWebServer.takeRequest())
                .matches(r -> "/Measure/foo/$bar".equals(r.getPath()), "path is '/Measure/foo/$bar'");
        assertThat(mockWebServer.takeRequest())
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath));
        assertThat(mockWebServer.takeRequest())
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath));
        assertThat(new String(result.getContent())).isEqualTo(body);
        assertThat(output.getOut()).contains("invalid Retry-After header value: five seconds");
    }

    @Test
    @DisplayName("Async FHIR client sends DELETE request to status location after polling exceeded client timeout")
    void asyncFhirClientSendsDeleteRequestAfterTimeout() throws Exception {
        var metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
                .getContentAsString(Charsets.UTF_8);
        var statusPath = "/Measure/foo/bar/status";
        var body = "bar";
        var metadataResponse = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
                .setBody(metadata);
        var kickOffResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of("Content-Location",
                        "http://localhost:%s%s".formatted(mockWebServer.getPort(), statusPath)))
                .setBody("");
        var inProgressResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of("X-Progress", "0%"))
                .setHeadersDelay(1, SECONDS)
                .setBody("");
        var deleteResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of("Content-Type", "text/plain"))
                .setBody(body);
        mockWebServer.enqueue(metadataResponse);
        mockWebServer.enqueue(kickOffResponse);
        mockWebServer.enqueue(inProgressResponse);
        mockWebServer.enqueue(inProgressResponse);
        mockWebServer.enqueue(deleteResponse);
        directSpringConfig = new DirectSpringConfig(true, null,
                "http://localhost:%s".formatted(mockWebServer.getPort()), null, null, null, null, null,
                Duration.ofSeconds(2), true);
        var client = directSpringConfig.getFhirClient(FhirContext.forR4());
        var result = client.operation().onInstance("Measure/foo").named("bar").withNoParameters(Parameters.class)
                .returnResourceType(Binary.class).execute();

        assertThat(result).matches(b -> b.hasData())
                .extracting(b -> new String(b.getData()))
                .asString()
                .isEqualTo(body);
        assertThat(mockWebServer.getRequestCount()).isEqualTo(5);
        assertThat(mockWebServer.takeRequest())
                .returns("/metadata", from(r -> r.getPath()))
                .returns("respond-async", from(r -> r.getHeader("Prefer")));
        assertThat(mockWebServer.takeRequest())
                .matches(r -> "/Measure/foo/$bar".equals(r.getPath()), "path is '/Measure/foo/$bar'");
        assertThat(mockWebServer.takeRequest())
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath));
        assertThat(mockWebServer.takeRequest())
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath));
        assertThat(mockWebServer.takeRequest())
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath))
                .matches(r -> r.getMethod().equals(HttpMethod.DELETE.name()),
                        "http method is '%s'".formatted(HttpMethod.DELETE.name()));
    }

    @Test
    @DisplayName("Async FHIR client handles thread interruption during polling")
    void asyncFhirClientHandlesInterruption() throws Exception {
        var metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
                .getContentAsString(Charsets.UTF_8);
        var statusPath = "/Measure/foo/bar/status";
        var body = "interrupted-response";
        var metadataResponse = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
                .setBody(metadata);
        var kickOffResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of("Content-Location",
                        "http://localhost:%s%s".formatted(mockWebServer.getPort(), statusPath)))
                .setBody("");
        // Long delay to ensure we're in sleep when interruption happens
        var inProgressResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of(HttpHeaders.RETRY_AFTER, "5"))
                .setBody("");
        var deleteResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of("Content-Type", "text/plain"))
                .setBody(body);
        mockWebServer.enqueue(metadataResponse);
        mockWebServer.enqueue(kickOffResponse);
        mockWebServer.enqueue(inProgressResponse);
        mockWebServer.enqueue(deleteResponse);
        directSpringConfig = new DirectSpringConfig(true, null,
                "http://localhost:%s".formatted(mockWebServer.getPort()), null, null, null, null, null,
                Duration.ofSeconds(10), true);
        var client = directSpringConfig.getFhirClient(FhirContext.forR4());

        // Create a second thread to execute the operation and interrupt it during polling
        Thread operationThread = new Thread(() -> {
            try {
                client.operation().onInstance("Measure/foo").named("bar").withNoParameters(Parameters.class)
                        .returnResourceType(Binary.class).execute();
            } catch (Exception e) {
                // Expect exception due to interruption
            }
        });

        // Start the operation and wait briefly to ensure it's in polling phase
        operationThread.start();
        Thread.sleep(500); // Wait for operation to start polling

        // Interrupt the thread during polling
        operationThread.interrupt();
        operationThread.join(); // Wait for thread to complete

        // Verify that DELETE request was sent
        assertThat(mockWebServer.getRequestCount()).isEqualTo(4);
        mockWebServer.takeRequest(); // Skip metadata request
        mockWebServer.takeRequest(); // Skip initial operation request
        mockWebServer.takeRequest(); // Skip first polling request

        // Verify the last request was a DELETE to the status endpoint
        RecordedRequest lastRequest = mockWebServer.takeRequest();
        assertThat(lastRequest)
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath))
                .matches(r -> r.getMethod().equals(HttpMethod.DELETE.name()),
                        "http method is '%s'".formatted(HttpMethod.DELETE.name()));
    }

    @Test
    @DisplayName("Async FHIR client handles non-202 response when canceling request")
    void asyncFhirClientHandlesNon202ResponseWhenCanceling(CapturedOutput output) throws Exception {
        var metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
                .getContentAsString(Charsets.UTF_8);
        var statusPath = "/Measure/foo/bar/status";
        var metadataResponse = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
                .setBody(metadata);
        var kickOffResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of("Content-Location",
                        "http://localhost:%s%s".formatted(mockWebServer.getPort(), statusPath)))
                .setBody("");
        var inProgressResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of(HttpHeaders.RETRY_AFTER, "5"))
                .setBody("");
        // Server returns 404 Not Found when trying to cancel the request
        var deleteResponse = new MockResponse().setResponseCode(404)
                .setHeaders(Headers.of("Content-Type", "text/plain"))
                .setBody("Not Found");
        mockWebServer.enqueue(metadataResponse);
        mockWebServer.enqueue(kickOffResponse);
        mockWebServer.enqueue(inProgressResponse);
        mockWebServer.enqueue(deleteResponse);
        directSpringConfig = new DirectSpringConfig(true, null,
                "http://localhost:%s".formatted(mockWebServer.getPort()), null, null, null, null, null,
                Duration.ofSeconds(1), true); // Short timeout to trigger cancellation
        var client = directSpringConfig.getFhirClient(FhirContext.forR4());

        // This should complete normally but log an error about failing to cancel
        assertThatThrownBy(
                () -> client.operation().onInstance("Measure/foo").named("bar").withNoParameters(Parameters.class)
                        .returnResourceType(Binary.class).execute())
                                .isInstanceOf(BaseServerResponseException.class);

        // Verify that DELETE request was sent
        assertThat(mockWebServer.getRequestCount()).isEqualTo(4);
        mockWebServer.takeRequest(); // Skip metadata request
        mockWebServer.takeRequest(); // Skip initial operation request
        mockWebServer.takeRequest(); // Skip first polling request

        // Verify the last request was a DELETE to the status endpoint
        var lastRequest = mockWebServer.takeRequest();
        assertThat(lastRequest)
                .matches(r -> statusPath.equals(r.getPath()), "path is '%s'".formatted(statusPath))
                .matches(r -> r.getMethod().equals(HttpMethod.DELETE.name()),
                        "http method is '%s'".formatted(HttpMethod.DELETE.name()));
        assertThat(output.getOut()).contains("Got http status 404 cancelling asynchronous request");
    }

    @Test
    @DisplayName("Async FHIR client handles HttpException when canceling request")
    void asyncFhirClientHandlesHttpExceptionWhenCanceling() throws Exception {
        var metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
                .getContentAsString(Charsets.UTF_8);
        var statusPath = "/Measure/foo/bar/status";
        var metadataResponse = new MockResponse().setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
                .setBody(metadata);
        var kickOffResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of("Content-Location",
                        "http://localhost:%s%s".formatted(mockWebServer.getPort(), statusPath)))
                .setBody("");
        var inProgressResponse = new MockResponse().setResponseCode(202)
                .setHeaders(Headers.of(HttpHeaders.RETRY_AFTER, "5"))
                .setBody("");
        // send invalid http status code
        var cancelResponse = new MockResponse().setResponseCode(42)
                .setBody("");

        mockWebServer.enqueue(metadataResponse);
        mockWebServer.enqueue(kickOffResponse);
        mockWebServer.enqueue(inProgressResponse);
        mockWebServer.enqueue(cancelResponse);

        directSpringConfig = new DirectSpringConfig(true, null,
                "http://localhost:%s".formatted(mockWebServer.getPort()), null, null, null, null, null,
                Duration.ofSeconds(1), true); // Short timeout to trigger cancellation
        var client = directSpringConfig.getFhirClient(FhirContext.forR4());

        assertThatThrownBy(
                () -> client.operation().onInstance("Measure/foo").named("bar").withNoParameters(Parameters.class)
                        .returnResourceType(Binary.class).execute())
                                .hasCauseInstanceOf(AsyncRequestException.class)
                                .cause().hasMessageContaining("Failed to cancel asynchronous request at");
    }

}
