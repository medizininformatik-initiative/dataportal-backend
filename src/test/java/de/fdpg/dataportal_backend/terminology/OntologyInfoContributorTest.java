package de.fdpg.dataportal_backend.terminology;

import de.fdpg.dataportal_backend.config.PropertiesReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.info.Info;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OntologyInfoContributorTest {

  private OntologyInfoContributor contributor;
  private Info.Builder builder;

  @Mock
  private PropertiesReader propertiesReader;

  @BeforeEach
  void setUp() {
    Mockito.reset(propertiesReader);
    builder = new Info.Builder();
    contributor = new OntologyInfoContributor(propertiesReader);
  }

  @Test
  void shouldAddOntologyTagToInfo_whenPropertiesReaderWorks() throws IOException {
    String expectedValue = "test-ontology-tag";
    doReturn(expectedValue).when(propertiesReader).getValue(OntologyInfoContributor.KEY_ONTOLOGY_TAG_PROPERTIES);

    contributor.contribute(builder);
    Info info = builder.build();

    Map<String, Object> details = info.getDetails();
    Map<String, String> terminologyObject = (Map<String, String>)details.get(OntologyInfoContributor.CATEGORY_TERMINOLOGY);
    assertEquals(expectedValue, terminologyObject.get(OntologyInfoContributor.KEY_ONTOLOGY_TAG_INFO));
  }
}
