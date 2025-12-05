package de.medizininformatikinitiative.dataportal.backend.query.ratelimiting;

import de.medizininformatikinitiative.dataportal.backend.config.WebSecurityConfig.Jwt2AuthoritiesConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Offer a method to check whether a given {@link Authentication} contains a certain authority/role.
 * <p>
 * Moved to a separate class in order to easily allow mocking responses in tests.
 */
@Component
public class AuthenticationHelper {

  private final Jwt2AuthoritiesConverter jwt2AuthoritiesConverter;

  @Autowired
  public AuthenticationHelper(Jwt2AuthoritiesConverter jwt2AuthoritiesConverter) {
    this.jwt2AuthoritiesConverter = jwt2AuthoritiesConverter;
  }

  /**
   * Check if a submitted {@link Authentication} contains the given authority.
   * <p>
   * {@link Authentication} must be of type {@link JwtAuthenticationToken}
   *
   * @param authentication an object that should be of type {@link JwtAuthenticationToken}
   * @param authority      the role/authority name to check
   * @return whether the principal contains the desired authority
   * @throws InvalidAuthenticationException in case some Object that is NOT a {@link JwtAuthenticationToken} is submitted
   */
  public boolean hasAuthority(Authentication authentication, String authority) throws InvalidAuthenticationException {
    if (authentication.getClass() == JwtAuthenticationToken.class) {
      var jwtPrincipal = (Jwt) authentication.getPrincipal();
      JwtAuthenticationToken jwtAuthenticationToken
          = new JwtAuthenticationToken(jwtPrincipal,
          jwt2AuthoritiesConverter.convert(jwtPrincipal));

      return jwtAuthenticationToken.getAuthorities().stream()
          .anyMatch(auth -> authority.equals(auth.getAuthority()));
    } else {
      throw new InvalidAuthenticationException();
    }
  }

}
