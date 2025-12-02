package de.fdpg.dataportal_backend.query.api.validation;

import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.resource.SchemaIdResolver;

public class JsonSchemaIdResolver implements SchemaIdResolver {

  public static final String SCHEMA_IRI_CRTDL_SCHEMA = "http://example.com/schema/data-extraction-schema.json";
  public static final String SCHEMA_IRI_CCDL_SCHEMA = "https://medizininformatik-initiative.de/fdpg/ClinicalCohortDefinitionLanguage/v1/schema";
  public static final String CLASSPATH_CRTDL_SCHEMA_FILE = "classpath:de/fdpg/dataportal_backend/query/api/validation/crtdl-schema.json";
  public static final String CLASSPATH_CCDL_SCHEMA_FILE = "classpath:de/fdpg/dataportal_backend/query/api/validation/ccdl-schema.json";

  @Override
  public AbsoluteIri resolve(AbsoluteIri absoluteIRI) {
    String iri = absoluteIRI.toString();
    if (SCHEMA_IRI_CRTDL_SCHEMA.equals(iri)) {
      return AbsoluteIri.of(CLASSPATH_CRTDL_SCHEMA_FILE);
    } else if (SCHEMA_IRI_CCDL_SCHEMA.equals(iri)) {
      return AbsoluteIri.of(CLASSPATH_CCDL_SCHEMA_FILE);
    }
    return null;
  }
}
