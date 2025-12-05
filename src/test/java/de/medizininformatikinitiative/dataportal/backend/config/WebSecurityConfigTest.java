package de.medizininformatikinitiative.dataportal.backend.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebSecurityConfigTest {

  private static final String KEY_REALM_ACCESS = "realm_access";
  private static final String KEY_RESOURCE_ACCESS = "resource_access";
  private static final String KEY_ROLES = "roles";
  private static final String KEY_SPRING_ADDONS_CONFIDENTIAL = "spring-addons-confidential";
  private static final String KEY_SPRING_ADDONS_PUBLIC = "spring-addons-public";

  @Test
  void shouldExtractAllRolesFromJwt() {
    Jwt jwt = mock(Jwt.class);

    Map<String, Object> realmAccess = Map.of(
        KEY_ROLES, List.of("role_realm1", "role_realm2")
    );

    Map<String, Object> confidentialAccess = Map.of(
        KEY_ROLES, List.of("role_conf1")
    );

    Map<String, Object> publicAccess = Map.of(
        KEY_ROLES, List.of("role_pub1", "role_pub2")
    );

    Map<String, Object> resourceAccess = Map.of(
        KEY_SPRING_ADDONS_CONFIDENTIAL, confidentialAccess,
        KEY_SPRING_ADDONS_PUBLIC, publicAccess
    );

    Map<String, Object> claims = Map.of(
        KEY_REALM_ACCESS, realmAccess,
        KEY_RESOURCE_ACCESS, resourceAccess
    );

    when(jwt.getClaims()).thenReturn(claims);

    var converter = new WebSecurityConfig();
    var authoritiesConverter = converter.authoritiesConverter();
    List<GrantedAuthority> authorities = (List<GrantedAuthority>) authoritiesConverter.convert(jwt);


    Assertions.assertNotNull(authorities);
    List<String> roles = authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

    Assertions.assertEquals(List.of("role_realm1", "role_realm2", "role_conf1", "role_pub1", "role_pub2"), roles);
  }
}
