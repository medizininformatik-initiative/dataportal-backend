package de.medizininformatikinitiative.dataportal.backend.config;

import de.medizininformatikinitiative.dataportal.backend.query.dataquery.DataqueryHandler;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.AuthenticationHelper;
import de.medizininformatikinitiative.dataportal.backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.medizininformatikinitiative.dataportal.backend.query.v5.DataqueryHandlerRestController;
import de.medizininformatikinitiative.dataportal.backend.terminology.TerminologyService;
import de.medizininformatikinitiative.dataportal.backend.terminology.es.TerminologyEsService;
import de.medizininformatikinitiative.dataportal.backend.terminology.v5.TerminologyRestController;
import de.medizininformatikinitiative.dataportal.backend.terminology.validation.StructuredQueryValidation;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// This is moved to a separate class to check if ssl enabled is correctly working
@ExtendWith(SpringExtension.class)
@Import({AuthenticationHelper.class,
    RateLimitingServiceSpringConfig.class,
    WebSecurityConfig.class
})
@WebMvcTest(
    controllers = {DataqueryHandlerRestController.class,
        TerminologyRestController.class
    }
)
@TestPropertySource(properties = {
    "server.ssl.enabled=true",
    "server.ssl.key-store=classpath:keystore.p12",
    "server.ssl.key-store-password=password",
    "server.ssl.key-store-type=PKCS12",
    "server.ssl.key-alias=tomcat"
})
class WebSecurityConfigSslIT {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private DataqueryHandler dataqueryHandler;

  @MockitoBean
  private StructuredQueryValidation structuredQueryValidation;

  @MockitoBean
  private TerminologyService terminologyService;

  @MockitoBean
  private TerminologyEsService terminologyEsService;

  @Test
  void shouldRedirectHttpToHttps() throws Exception {
    mockMvc.perform(get(URI.create("/api/v5/query/data")).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", Matchers.startsWith("https://")));
  }

  @Test
  @WithMockUser(username = "user", roles = "DATAPORTAL_TEST_USER")
  void shouldNotRedirectHttps() throws Exception {
    mockMvc.perform(get(URI.create("https://localhost/api/v5/query/data")).with(csrf()))
        .andExpect(status().isOk());
  }

}
