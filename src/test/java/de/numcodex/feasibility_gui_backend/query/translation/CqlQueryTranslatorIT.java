package de.numcodex.feasibility_gui_backend.query.translation;

import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Unit;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.query.api.AttributeFilter;
import de.numcodex.feasibility_gui_backend.query.api.Ccdl;
import de.numcodex.feasibility_gui_backend.query.api.TimeRestriction;
import de.numcodex.feasibility_gui_backend.query.api.ValueFilter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.util.List;

import static de.numcodex.feasibility_gui_backend.common.api.Comparator.GREATER_EQUAL;
import static de.numcodex.feasibility_gui_backend.query.api.ValueFilterType.CONCEPT;
import static de.numcodex.feasibility_gui_backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Tag("query")
@Tag("translation")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = QueryTranslatorSpringConfig.class,
    properties = {
        "app.cqlTranslationEnabled=true",
        "app.fhirTranslationEnabled=false"
    }
)
@SuppressWarnings("NewClassNamingConvention")
public class CqlQueryTranslatorIT {

  @Autowired
  @Qualifier("cql")
  private QueryTranslator cqlQueryTranslator;

  @Test
  public void testTranslate() {
    var termCode = TermCode.builder()
        .code("424144002")
        .system("http://snomed.info/sct")
        .display("Gegenwärtiges chronologisches Alter")
        .build();
    var context = TermCode.builder()
        .code("Patient")
        .system("fdpg.mii.cds")
        .version("1.0.0")
        .display("Patient")
        .build();
    var unit = Unit.builder()
        .code("a")
        .display("a")
        .build();
    var valueFilter = ValueFilter.builder()
        .type(QUANTITY_COMPARATOR)
        .comparator(GREATER_EQUAL)
        .quantityUnit(unit)
        .value(50.0)
        .build();
    var criterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .context(context)
        .valueFilter(valueFilter)
        .build();
    var testQuery = Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(criterion)))
        .build();

    var translationResult = assertDoesNotThrow(() -> cqlQueryTranslator.translate(testQuery));
    assertThat(translationResult).isInstanceOf(String.class);
    assertThat(translationResult).containsIgnoringCase("Context Patient");
  }

  @Test
  public void testTranslate_SupportsTimeRestrictions() {
    var termCode = TermCode.builder()
        .code("I118048")
        .system("http://fhir.de/CodeSystem/bfarm/alpha-id")
        .display("18-Hydroxylase-Mangel")
        .version("2025")
        .build();
    var context = TermCode.builder()
        .code("Diagnose")
        .display("Diagnose")
        .system("fdpg.mii.cds")
        .version("1.0.0")
        .build();
    var timeRestriction = new TimeRestriction("2021-10-09", "2021-09-09");
    var criterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .timeRestriction(timeRestriction)
        .context(context)
        .build();
    var testQuery = Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(criterion)))
        .build();

    var translationResult = assertDoesNotThrow(() -> cqlQueryTranslator.translate(testQuery));
    assertThat(translationResult).isInstanceOf(String.class);
    assertThat(translationResult).containsIgnoringCase("Context Patient");
  }

  @Test
  public void testTranslate_SupportsAttributeFilters() {
    var termCode = TermCode.builder()
        .code("IMP")
        .system("http://terminology.hl7.org/CodeSystem/v3-ActCode")
        .display("inpatient encounter")
        .version("9.0.0")
        .build();
    var context = TermCode.builder()
        .code("Fall")
        .display("Fall")
        .system("fdpg.mii.cds")
        .version("1.0.0")
        .build();
    var attributeCode1 = TermCode.builder()
        .code("Fachabteilungsschluessel")
        .display("Fachabteilungsschluessel")
        .system("http://hl7.org/fhir/StructureDefinition")
        .build();
    var attributeCode2 = TermCode.builder()
        .code("ErweiterterFachabteilungsschluessel")
        .display("ErweiterterFachabteilungsschluessel")
        .system("http://hl7.org/fhir/StructureDefinition")
        .build();
    var concept1 = TermCode.builder()
        .code("3400")
        .display("Dermatologie")
        .system("http://fhir.de/CodeSystem/dkgev/Fachabteilungsschluessel")
        .build();
    var concept2 = TermCode.builder()
        .code("1500")
        .display("Allgemeine Chirurgie")
        .system("http://fhir.de/CodeSystem/dkgev/Fachabteilungsschluessel")
        .build();
    var attributeFilter1 = AttributeFilter.builder()
        .attributeCode(attributeCode1)
        .type(CONCEPT)
        .selectedConcepts(List.of(concept1))
        .build();
    var attributeFilter2 = AttributeFilter.builder()
        .attributeCode(attributeCode2)
        .type(CONCEPT)
        .selectedConcepts(List.of(concept2))
        .build();
    var criterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .context(context)
        .attributeFilters(List.of(attributeFilter1, attributeFilter2))
        .build();

    var testQuery = Ccdl.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(criterion)))
        .build();

    var translationResult = assertDoesNotThrow(() -> cqlQueryTranslator.translate(testQuery));
    assertThat(translationResult).isInstanceOf(String.class);
    assertThat(translationResult).containsIgnoringCase("Context Patient");
  }
}
