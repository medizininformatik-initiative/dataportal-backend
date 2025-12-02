package de.fdpg.dataportal_backend.query.broker.dsf;

import ca.uhn.fhir.context.FhirContext;
import de.fdpg.dataportal_backend.query.broker.BrokerClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.threeten.extra.PeriodDuration;


/**
 * Spring configuration for providing a {@link DSFBrokerClient} instance.
 */
@Lazy
@Configuration
public class DSFSpringConfig {

    @Value("${app.broker.dsf.security.client.certificate}")
    private String clientCertificateFile;

    @Value("${app.broker.dsf.security.client.key.file}")
    private String clientKeyFile;

    @Value("${app.broker.dsf.security.client.key.password}")
    private char[] keyStorePassword;

    @Value("${app.broker.dsf.security.certificate}")
    private String certificateFile;

    @Value("${app.broker.dsf.proxy.host}")
    private String proxyHost;

    @Value("${app.broker.dsf.proxy.username}")
    private String proxyUsername;

    @Value("${app.broker.dsf.proxy.password}")
    private String proxyPassword;

    @Value("${app.broker.dsf.webservice.baseUrl}")
    private String webserviceBaseUrl;

    @Value("${app.broker.dsf.webservice.readTimeout}")
    private String webserviceReadTimeout;

    @Value("${app.broker.dsf.webservice.connectTimeout}")
    private String webserviceConnectTimeout;

    @Value("${app.broker.dsf.webservice.logRequests}")
    private boolean logRequests;

    @Value("${app.broker.dsf.websocket.url}")
    private String websocketUrl;

    @Value("${app.broker.dsf.organizationId}")
    private String organizationId;

    @Qualifier("dsf")
    @Bean
    public BrokerClient dsfBrokerClient(QueryManager queryManager, QueryResultCollector queryResultCollector) {
        return new DSFBrokerClient(queryManager, queryResultCollector);
    }

    @Bean
    QueryManager dsfQueryManager(FhirWebClientProvider fhirWebClientProvider, DSFMediaTypeTranslator dsfMediaTypeTranslator) {
        return new DSFQueryManager(fhirWebClientProvider, dsfMediaTypeTranslator,
                organizationId.replace(' ', '_'));
    }

    @Bean
    DSFMediaTypeTranslator dsfMediaTypeTranslator() {
        return new DSFMediaTypeTranslator();
    }

    @Bean
    QueryResultCollector queryResultCollector(QueryResultStore resultStore, FhirContext fhirContext,
                                              FhirWebClientProvider webClientProvider, DSFQueryResultHandler resultHandler) {
        return new DSFQueryResultCollector(resultStore, fhirContext, webClientProvider, resultHandler);
    }

    @Bean
    QueryResultStore queryResultStore() {
        return new DSFQueryResultStore();
    }

    @Bean
    DSFQueryResultHandler queryResultHandler(FhirWebClientProvider webClientProvider) {
        return new DSFQueryResultHandler(webClientProvider);
    }


    @Bean
    FhirSecurityContextProvider fhirSecurityContextProvider() {
        return new DSFFhirSecurityContextProvider(clientCertificateFile, clientKeyFile, keyStorePassword,
                certificateFile);
    }

    @Bean
    FhirProxyContext fhirProxyContext() {
        return new FhirProxyContext(
                (proxyHost.isEmpty()) ? null : proxyHost,
                (proxyUsername.isEmpty()) ? null : proxyUsername,
                (proxyPassword.isEmpty()) ? null : proxyPassword
        );
    }

    @Bean
    FhirWebClientProvider fhirWebClientProvider(FhirContext fhirContext,
                                                FhirSecurityContextProvider securityContextProvider,
                                                FhirProxyContext proxyContext) {
        // FhirWebserviceClientJersey.class takes int as timeout parameter, so we need to cast long to int at some point
        return new DSFFhirWebClientProvider(fhirContext, webserviceBaseUrl, (int) PeriodDuration.parse(webserviceReadTimeout).getDuration().toMillis(),
            (int)PeriodDuration.parse(webserviceConnectTimeout).getDuration().toMillis(), websocketUrl, securityContextProvider, proxyContext, logRequests);
    }

}
