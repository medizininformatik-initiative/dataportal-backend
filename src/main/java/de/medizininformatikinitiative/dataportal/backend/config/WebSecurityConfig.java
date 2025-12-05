package de.medizininformatikinitiative.dataportal.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = false)
public class WebSecurityConfig {

  public static final String KEY_REALM_ACCESS = "realm_access";
  public static final String KEY_ROLES = "roles";
  public static final String KEY_RESOURCE_ACCESS = "resource_access";
  public static final String KEY_SPRING_ADDONS_CONFIDENTIAL = "spring-addons-confidential";
  public static final String KEY_SPRING_ADDONS_PUBLIC = "spring-addons-public";
  public static final String PATH_ACTUATOR_HEALTH = "/actuator/health";
  public static final String PATH_ACTUATOR_INFO = "/actuator/info";
  public static final String PATH_API = "/api/v5";
  public static final String PATH_QUERY = "/query";
  public static final String PATH_DATA = "/data";
  public static final String PATH_FEASIBILITY = "/feasibility";
  public static final String PATH_ID_MATCHER = "/{id:\\d+}";
  public static final String PATH_USER_ID_MATCHER = "/by-user/{id:[\\w-]+}";
  public static final String PATH_CRTDL = "/crtdl";
  public static final String PATH_SUMMARY_RESULT = "/summary-result";
  public static final String PATH_DETAILED_OBFUSCATED_RESULT = "/detailed-obfuscated-result";
  public static final String PATH_DETAILED_RESULT = "/detailed-result";
  public static final String PATH_TERMINOLOGY = "/terminology";
  public static final String PATH_DSE = "/dse";
  public static final String PATH_CODEABLE_CONCEPT = "/codeable-concept";
  public static final String PATH_SWAGGER_UI = "/swagger-ui/**";
  public static final String PATH_SWAGGER_CONFIG = "/v3/api-docs/**";
  public static final String PATH_SETTINGS = "/.settings";
  @Value("${app.keycloakAllowedRole}")
  private String keycloakAllowedRole;

  @Value("${app.keycloakAdminRole}")
  private String keycloakAdminRole;

  @SuppressWarnings("unchecked")
  @Bean
  public Jwt2AuthoritiesConverter authoritiesConverter() {
    return jwt -> {
      final var realmAccess = (Map<String, Object>) jwt.getClaims().getOrDefault(KEY_REALM_ACCESS, Map.of());
      final var realmRoles = (Collection<String>) realmAccess.getOrDefault(KEY_ROLES, List.of());

      final var resourceAccess = (Map<String, Object>) jwt.getClaims().getOrDefault(
          KEY_RESOURCE_ACCESS, Map.of());
      final var confidentialClientAccess = (Map<String, Object>) resourceAccess.getOrDefault(
          KEY_SPRING_ADDONS_CONFIDENTIAL, Map.of());
      final var confidentialClientRoles = (Collection<String>) confidentialClientAccess.getOrDefault(KEY_ROLES, List.of());
      final var publicClientAccess = (Map<String, Object>) resourceAccess.getOrDefault(
          KEY_SPRING_ADDONS_PUBLIC, Map.of());
      final var publicClientRoles = (Collection<String>) publicClientAccess.getOrDefault(KEY_ROLES, List.of());

      return Stream.concat(realmRoles.stream(), Stream.concat(confidentialClientRoles.stream(), publicClientRoles.stream()))
          .map(SimpleGrantedAuthority::new).toList();
    };
  }

  @Bean
  public Jwt2AuthenticationConverter authenticationConverter(Jwt2AuthoritiesConverter authoritiesConverter) {
    return jwt -> new JwtAuthenticationToken(jwt, authoritiesConverter.convert(jwt));
  }

  @Bean
  public SecurityFilterChain apiFilterChain(
      HttpSecurity http,
      ServerProperties serverProperties,
      Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter) throws Exception {

    http.authorizeHttpRequests(authorize -> authorize
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_SETTINGS)).permitAll()
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_SWAGGER_CONFIG)).permitAll()
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_API + PATH_ACTUATOR_HEALTH)).permitAll()
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_API + PATH_ACTUATOR_INFO)).permitAll()
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_API + PATH_SWAGGER_UI)).permitAll()
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_API + PATH_TERMINOLOGY + "/**")).hasAuthority(keycloakAllowedRole)
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_API + PATH_QUERY + PATH_DATA)).hasAuthority(keycloakAllowedRole)
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_API + PATH_QUERY + PATH_DATA + PATH_USER_ID_MATCHER)).hasAuthority(keycloakAdminRole)
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_API + PATH_QUERY + PATH_DATA + "/*")).hasAnyAuthority(keycloakAdminRole, keycloakAllowedRole)
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_API + PATH_QUERY + PATH_FEASIBILITY)).hasAuthority(keycloakAllowedRole)
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_API + PATH_QUERY + PATH_FEASIBILITY + PATH_ID_MATCHER + PATH_DETAILED_RESULT)).hasAuthority(keycloakAdminRole)
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_API + "/**")).hasAnyAuthority(keycloakAdminRole, keycloakAllowedRole)
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_API + PATH_DSE + "/**")).hasAnyAuthority(keycloakAdminRole, keycloakAllowedRole)
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(PATH_API + PATH_CODEABLE_CONCEPT + "/**")).hasAnyAuthority(keycloakAdminRole, keycloakAllowedRole)
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(authenticationConverter)
            )
        )
        .cors(Customizer.withDefaults())
        .anonymous(Customizer.withDefaults())
        .sessionManagement((session) -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

    if (serverProperties.getSsl() != null && serverProperties.getSsl().isEnabled()) {
      http.redirectToHttps(Customizer.withDefaults());
    }
    return http.build();
  }

  public interface Jwt2AuthoritiesConverter extends
      Converter<Jwt, Collection<? extends GrantedAuthority>> {
  }

  public interface Jwt2AuthenticationConverter extends Converter<Jwt, JwtAuthenticationToken> {
  }
}
