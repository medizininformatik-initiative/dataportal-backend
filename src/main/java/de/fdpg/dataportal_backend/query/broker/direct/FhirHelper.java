package de.fdpg.dataportal_backend.query.broker.direct;

import static de.fdpg.dataportal_backend.query.QueryMediaType.CQL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.io.InputStream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.springframework.stereotype.Component;

@Component
public class FhirHelper {

  private final FhirContext context;

  public FhirHelper(FhirContext context) {
    this.context = context;
  }


  /**
   * Create a {@link Bundle} with predefined library and measure URI, as well as CQL String
   * @param cql the plaintext cql definition
   * @param libraryUri a library uri {@link String} to be included in the {@link Bundle}
   * @param measureUri a measure uri {@link String} to be included in the {@link Bundle}
   * @return the {@link Bundle}, consisting of a {@link Library} and {@link Measure}, containing the submitted values
   */
  public Bundle createBundle(String cql, String libraryUri, String measureUri) throws IOException {
    var library = appendCql(parseResource(Library.class,
        getResourceFileAsString("Library.json")).setUrl(libraryUri), cql);
    var measure = parseResource(Measure.class,
        getResourceFileAsString("Measure.json"))
        .setUrl(measureUri)
        .addLibrary(libraryUri);
    return bundleLibraryAndMeasure(library, measure);
  }

  /**
   * Parse a String as an {@link IBaseResource} implementation
   * @param type the concrete {@link IBaseResource} implementation class to parse to
   * @param input the {@link String} to parse
   * @return the wanted {@link IBaseResource} implementation object
   * @param <T> any implementation of {@link IBaseResource}
   */
  private <T extends IBaseResource> T parseResource(Class<T> type, String input) {
    var parser = context.newJsonParser();
    return type.cast(parser.parseResource(input));
  }

  /**
   * Add the CQL query to a {@link Library}
   * @param library the {@link Library} to add the CQL string to
   * @param cql the CQL string to add
   * @return the {@link Library} with the added CQL
   */
  private static Library appendCql(Library library, String cql) {
    library.getContentFirstRep().setContentType(CQL.getRepresentation());
    library.getContentFirstRep().setData(cql.getBytes(UTF_8));
    return library;
  }

  /**
   * Create a {@link Bundle} of a {@link Library} and a {@link Measure}
   * @param library the {@link Library} to add to the {@link Bundle}
   * @param measure the {@link Measure} to add to the {@link Bundle}
   * @return the {@link Bundle}, consisting of the given {@link Library} and {@link Measure}
   */
  private static Bundle bundleLibraryAndMeasure(Library library, Measure measure) {
    var bundle = new Bundle();
    bundle.setType(TRANSACTION);
    bundle.addEntry().setResource(library).getRequest().setMethod(POST).setUrl("Library");
    bundle.addEntry().setResource(measure).getRequest().setMethod(POST).setUrl("Measure");
    return bundle;
  }

  /**
   * Read file contents as String
   * @param fileName name of the resource file
   * @return the String contents of the file
   */
  public static String getResourceFileAsString(String fileName) throws IOException {
    InputStream is = getResourceFileAsInputStream(fileName);
    if (is != null) {
      return new String(is.readAllBytes(), UTF_8);
    } else {
      throw new RuntimeException("File not found in classpath: " + fileName);
    }
  }

  /**
   * Read file contents as {@link InputStream}
   * @param fileName name of the resource file
   * @return an {@link InputStream} of the file
   */
  private static InputStream getResourceFileAsInputStream(String fileName) {
    return DirectBrokerClientCql.class.getResourceAsStream(fileName);
  }
}
