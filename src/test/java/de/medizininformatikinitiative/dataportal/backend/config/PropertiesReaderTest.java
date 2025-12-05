package de.medizininformatikinitiative.dataportal.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static de.medizininformatikinitiative.dataportal.backend.terminology.OntologyInfoContributor.KEY_ONTOLOGY_TAG_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class PropertiesReaderTest {

  PropertiesReader reader;
  @Mock
  private Environment env;

  @BeforeEach
  void setUp() {
    Mockito.reset(env);
    reader = new PropertiesReader(env);
  }

  @Test
  void testGetProperty() {
    String versionInfo = "v1.0.0";
    doReturn(versionInfo).when(env).getProperty(KEY_ONTOLOGY_TAG_PROPERTIES);

    String ontologyTagProperty = reader.getValue(KEY_ONTOLOGY_TAG_PROPERTIES);
    assertThat(ontologyTagProperty).isNotBlank();
    assertThat(ontologyTagProperty).isEqualTo(versionInfo);
  }
}
