package de.numcodex.feasibility_gui_backend.query.broker.dsf;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.client.FhirWebserviceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
public class DSFFhirWebClientProviderTest {

    @Container
    private GenericContainer<?> blaze = new GenericContainer<>("samply/blaze:1.1.2")
            .withExposedPorts(8080)
            .withNetwork(Network.newNetwork())
            .withReuse(true);

    @Test
    void settingLogRequestsFlagToTrueEnablesRequestLogs(CapturedOutput output) throws Exception {
        String webserviceBaseUrl = format("http://%s:%s/fhir", blaze.getHost(), blaze.getFirstMappedPort());
        String websocketBaseUrl = format("ws://%s:%s/fhir", blaze.getHost(), blaze.getFirstMappedPort());
        FhirSecurityContextProvider securityContextProvider = () -> new FhirSecurityContext(null, null, null);
        FhirProxyContext proxyContext = new FhirProxyContext(null, null, null);
        DSFFhirWebClientProvider clientProvider = new DSFFhirWebClientProvider(FhirContext.forR4(), webserviceBaseUrl,
                20000, 2000, websocketBaseUrl, securityContextProvider, proxyContext, true);
        FhirWebserviceClient client = clientProvider.provideFhirWebserviceClient();

        client.getConformance();

        assertThat(output)
                .containsIgnoringCase("sending client request")
                .containsIgnoringCase("client response received");
    }

    @Test
    void settingLogRequestsFlagToFalseDisablesRequestLogs(CapturedOutput output) throws Exception {
        String webserviceBaseUrl = format("http://%s:%s/fhir", blaze.getHost(), blaze.getFirstMappedPort());
        String websocketBaseUrl = format("ws://%s:%s/fhir", blaze.getHost(), blaze.getFirstMappedPort());
        FhirSecurityContextProvider securityContextProvider = () -> new FhirSecurityContext(null, null, null);
        FhirProxyContext proxyContext = new FhirProxyContext(null, null, null);
        DSFFhirWebClientProvider clientProvider = new DSFFhirWebClientProvider(FhirContext.forR4(), webserviceBaseUrl,
                20000, 2000, websocketBaseUrl, securityContextProvider, proxyContext, false);
        FhirWebserviceClient client = clientProvider.provideFhirWebserviceClient();

        client.getConformance();

        assertThat(output)
                .doesNotContainIgnoringCase("sending client request")
                .doesNotContainIgnoringCase("client response received");
    }

}
