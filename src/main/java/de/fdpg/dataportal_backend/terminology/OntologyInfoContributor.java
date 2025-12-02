package de.fdpg.dataportal_backend.terminology;

import de.fdpg.dataportal_backend.config.PropertiesReader;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class OntologyInfoContributor implements InfoContributor {

  public static final String CATEGORY_TERMINOLOGY = "terminology";
  public static final String KEY_ONTOLOGY_TAG_PROPERTIES = "ontology-tag";
  public static final String KEY_ONTOLOGY_TAG_INFO = "ontologyTag";

  private final PropertiesReader propertiesReader;

  public OntologyInfoContributor(PropertiesReader propertiesReader) {
    this.propertiesReader = propertiesReader;
  }

  @Override
  public void contribute(Info.Builder builder) {
    builder.withDetail(CATEGORY_TERMINOLOGY, Collections.singletonMap(KEY_ONTOLOGY_TAG_INFO, propertiesReader.getValue(KEY_ONTOLOGY_TAG_PROPERTIES)));
  }
}
