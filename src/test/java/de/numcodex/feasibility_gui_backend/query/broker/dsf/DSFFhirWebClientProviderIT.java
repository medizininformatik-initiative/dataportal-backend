package de.numcodex.feasibility_gui_backend.query.broker.dsf;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
public class DSFFhirWebClientProviderIT {

    private static final String DB_PASSWORD = "foo-005331";
    private static final Network network = Network.newNetwork();

    @Container
    private PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:18-alpine")
                    .withNetwork(network)
                    .withNetworkAliases("db")
                    .withDatabaseName("fhir")
                    .withUsername("liquibase_user")
                    .withPassword(DB_PASSWORD);
    
    @Container
    private GenericContainer<?> dsfFhir = new GenericContainer<>("ghcr.io/datasharingframework/fhir:1.9.0")
            .withExposedPorts(8080)
            .withNetwork(network)
            .withNetworkAliases("dsf-fhir")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(getResource("server.crt"), 444),
                    "/certs/server.crt")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(getResource("client.crt"), 444),
                    "/certs/client.crt")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(getResource("client.key"), 444),
                    "/certs/client.key")
            .withEnv("DEV_DSF_SERVER_AUTH_TRUST_CLIENT_CERTIFICATE_CAS", "/certs/server.crt")
            .withEnv("DEV_DSF_FHIR_CLIENT_CERTIFICATE", "/certs/client.crt")
            .withEnv("DEV_DSF_FHIR_CLIENT_CERTIFICATE_PRIVATE_KEY", "/certs/client.key")
            .withEnv("DEV_DSF_FHIR_CLIENT_CERTIFICATE_PRIVATE_KEY_PASSWORD", "password")
            .withEnv("DEV_DSF_FHIR_DB_URL", "jdbc:postgresql://db/fhir")
            .withEnv("DEV_DSF_FHIR_DB_LIQUIBASE_PASSWORD", DB_PASSWORD)
            .withEnv("DEV_DSF_FHIR_DB_USER_PASSWORD", DB_PASSWORD)
            .withEnv("DEV_DSF_FHIR_DB_USER_PERMANENT_DELETE_PASSWORD", DB_PASSWORD)
            .withEnv("DEV_DSF_FHIR_SERVER_ORGANIZATION_IDENTIFIER_VALUE", "websocket-test")
            .withEnv("DEV_DSF_FHIR_SERVER_BASE_URL", "https://proxy:8443/fhir")
            .withEnv("DEV_DSF_FHIR_SERVER_ORGANIZATION_THUMBPRINT",
                    "14d59cf5a56a1497128b4190d38d3c08002340890f900f94a1cdc432ad5a616f74d5eb4a91ad495c7044bf0145eca0f97d809b86fe7d52d2ebfa4f3d132f7249")
            .withReuse(true)
            .dependsOn(db);

    @Container
    private GenericContainer<?> tlsProxy = new GenericContainer<>("nginx:1.29-alpine")
            .withExposedPorts(8443)
            .withNetwork(network)
            .withNetworkAliases("proxy")
            .withCopyFileToContainer(MountableFile.forHostPath(getResource("server.crt"), 444),
                    "/etc/nginx/certs/server.crt")
            .withCopyFileToContainer(MountableFile.forHostPath(getResource("server.key"), 400),
                    "/etc/nginx/certs/server.key")
            .withCopyFileToContainer(MountableFile.forHostPath(getResource("proxy.conf"), 444),
                    "/etc/nginx/conf.d/default.conf")
            .withReuse(true)
            .dependsOn(dsfFhir);

    private String getResource(String file) {
        return Objects.requireNonNull(DSFFhirWebClientProviderIT.class.getResource(file),
                "Resource file '%s' not found".formatted(file)).getPath();
    }

    @Test
    void websocketReconnectsAfterConnectionTerminates(CapturedOutput output) throws Exception {
        String webserviceBaseUrl = format("https://%s:%s/fhir", tlsProxy.getHost(), tlsProxy.getFirstMappedPort());
        String websocketBaseUrl = format("wss://%s:%s/fhir/ws", tlsProxy.getHost(), tlsProxy.getFirstMappedPort());
        FhirSecurityContextProvider securityContextProvider = new DSFFhirSecurityContextProvider(
                getResource("client.crt"), getResource("client.key"), "password".toCharArray(),
                getResource("server.crt"));
        FhirProxyContext proxyContext = new FhirProxyContext(null, null, null);
        DSFFhirWebClientProvider clientProvider = new DSFFhirWebClientProvider(FhirContext.forR4(), webserviceBaseUrl,
                20000, 2000, websocketBaseUrl, securityContextProvider, proxyContext, false);
        var wsClient = clientProvider.provideFhirWebsocketClient();

        wsClient.connect();
        // give some time to establish
        Thread.sleep(2000L);
        // simulate remote termination by stopping and restarting the DSF-FHIR container
        // wait a bit in between for the client to detect the broken connection and trigger reconnect attempts
        dsfFhir.stop();
        Thread.sleep(3000L);
        dsfFhir.start();

        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(output.toString()).contains("Websocket connection recovered"));
        wsClient.disconnect();
    }
}
