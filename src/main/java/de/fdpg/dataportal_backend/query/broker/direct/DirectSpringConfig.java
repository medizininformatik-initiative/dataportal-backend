package de.fdpg.dataportal_backend.query.broker.direct;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import de.fdpg.dataportal_backend.query.broker.BrokerClient;
import de.fdpg.dataportal_backend.query.broker.NoOpInterceptor;
import de.fdpg.dataportal_backend.query.broker.OAuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

/**
 * Spring configuration for providing a {@link DirectBrokerClient} implementation instance. Either
 * {@link DirectBrokerClientCql} or {@link DirectBrokerClientFlare}
 */
@Lazy
@Configuration
@Slf4j
public class DirectSpringConfig {

    private final boolean useCql;
    private final String flareBaseUrl;
    private final String cqlBaseUrl;
    private final String username;
    private final String password;
    private final Duration timeout;
    private String issuer;
    private String clientId;
    private String clientSecret;
    private boolean useAsyncRequestPattern;

    public DirectSpringConfig(@Value("${app.broker.direct.cql.enabled:false}") boolean useCql,
            @Value("${app.broker.direct.flare.baseUrl:}") String flareBaseUrl, @Value("${app.broker.direct.cql.baseUrl:}") String cqlBaseUrl,
            @Value("${app.broker.direct.auth.basic.username:}") String username,
            @Value("${app.broker.direct.auth.basic.password:}") String password,
            @Value("${app.broker.direct.auth.oauth.issuer.url:}") String issuer,
            @Value("${app.broker.direct.auth.oauth.client.id:}") String clientId,
            @Value("${app.broker.direct.auth.oauth.client.secret:}") String clientSecret,
            @Value("#{T(java.time.Duration).parse('${app.broker.direct.timeout:PT20S}')}") Duration timeout,
            @Value("${app.broker.direct.cql.useAsyncRequestPattern:false}") boolean useAsyncRequestPattern) {
        this.useCql = useCql;
        this.flareBaseUrl = flareBaseUrl;
        this.cqlBaseUrl = cqlBaseUrl;
        this.username = username;
        this.password = password;
        this.issuer = issuer;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.timeout = timeout;
        this.useAsyncRequestPattern = useAsyncRequestPattern;
    }

    @Qualifier("direct")
    @Bean
    public BrokerClient directBrokerClient(WebClient directWebClientFlare,
                                           @Value("${app.broker.direct.obfuscateResultCount:false}") boolean obfuscateResultCount,
                                           FhirConnector fhirConnector, FhirHelper fhirHelper) {
        if (useCql) {
            log.info("Enable direct broker (type: cql)");
            return new DirectBrokerClientCql(fhirConnector, obfuscateResultCount, fhirHelper);
        } else {
            log.info("Enable direct broker (type: flare)");
            return new DirectBrokerClientFlare(directWebClientFlare, obfuscateResultCount);
        }
    }

    @Bean
    public IGenericClient getFhirClient(FhirContext fhirContext) {
        var timeoutMs = (int) Math.min(timeout.toMillis(), Integer.MAX_VALUE);
        var clientFactory = fhirContext.getRestfulClientFactory();
        fhirContext.setRestfulClientFactory(clientFactory);
        var httpClient = HttpClientBuilder.create()
                .setRequestExecutor(
                        useAsyncRequestPattern ? new AsyncRequestExecutor() : new HttpRequestExecutor())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setSocketTimeout(timeoutMs).build())
                .build();
        clientFactory.setSocketTimeout(timeoutMs);
        clientFactory.setHttpClient(httpClient);
        var client = fhirContext.newRestfulGenericClient(cqlBaseUrl);
        client.registerInterceptor(getAuth());

        return client;
    }

    private IClientInterceptor getAuth() {
        if (!isNullOrEmpty(password) && !isNullOrEmpty(username)) {
            log.info("Configure direct broker instance with basic authentication (username: {})", username);
            return new BasicAuthInterceptor(username, password);
        } else if (!isNullOrEmpty(issuer) && !isNullOrEmpty(clientId) && !isNullOrEmpty(clientSecret)) {
            log.info("Configure direct broker instance with oauth authentication (issuer: {}, client-id: {})", issuer,
                    clientId);
            return new OAuthInterceptor(issuer, clientId, clientSecret);
        } else {
            return new NoOpInterceptor();
        }
    }

    @Bean
    public WebClient directWebClientFlare() {
        var clientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(timeout)))
                .baseUrl(flareBaseUrl);

        if (!isNullOrEmpty(password) && !isNullOrEmpty(username)) {
            log.info("Configure direct broker instance with basic authentication"
                    + " (type: flare, url: {}, username: {}, timeout: {})",
                    flareBaseUrl, username, timeout);
            return clientBuilder
                    .filter(basicAuthentication(username, password))
                    .build();
        } else {
            log.info("Configure direct broker instance (type: flare, url: {}, timeout: {})", flareBaseUrl, timeout);
            return clientBuilder.build();
        }
    }
}
