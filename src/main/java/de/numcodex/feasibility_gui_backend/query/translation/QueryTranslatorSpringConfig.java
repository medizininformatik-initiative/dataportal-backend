package de.numcodex.feasibility_gui_backend.query.translation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.numcodex.feasibility_gui_backend.query.QueryMediaType;
import de.numcodex.sq2cql.Translator;
import de.numcodex.sq2cql.model.Mapping;
import de.numcodex.sq2cql.model.MappingContext;
import de.numcodex.sq2cql.model.MappingTreeBase;
import de.numcodex.sq2cql.model.MappingTreeModuleRoot;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.CQL;
import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.FHIR;
import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.STRUCTURED_QUERY;
import static java.util.Map.entry;

@Configuration
public class QueryTranslatorSpringConfig {

    @Value("${app.mappingsFile}")
    private String mappingsFile;

    @Value("${app.conceptTreeFile}")
    private String conceptTreeFile;

    @Value("${app.flare.baseUrl:}")
    private String flareBaseUrl;

    @Value("${app.cqlTranslationEnabled:true}")
    private boolean cqlTranslationEnabled;

    @Value("${app.fhirTranslationEnabled:true}")
    private boolean fhirTranslationEnabled;

    private final ApplicationContext appContext;

    public QueryTranslatorSpringConfig(ApplicationContext appContext) {
        this.appContext = Objects.requireNonNull(appContext);
    }

    private QueryTranslator getQueryTranslatorByQualifier(String qualifier) {
        return BeanFactoryAnnotationUtils
                .qualifiedBeanOfType(appContext.getAutowireCapableBeanFactory(), QueryTranslator.class, qualifier);
    }

    @Bean
    QueryTranslationComponent createQueryTranslationService(@Qualifier("json") QueryTranslator jsonQueryTranslator) {
        var queryTranslators = new HashMap<QueryMediaType, QueryTranslator>();
        queryTranslators.put(STRUCTURED_QUERY, jsonQueryTranslator);

        if (fhirTranslationEnabled) {
            queryTranslators.put(FHIR, getQueryTranslatorByQualifier("fhir"));
        }

        if (cqlTranslationEnabled) {
            queryTranslators.put(CQL, getQueryTranslatorByQualifier("cql"));
        }
        return new QueryTranslationComponent(queryTranslators);
    }

    @Lazy
    @Bean
    Translator createCqlTranslator(@Qualifier("translation") ObjectMapper jsonUtil) throws IOException {
        var mappings = jsonUtil.readValue(new File(mappingsFile), Mapping[].class);
        var mappingTreeBase = new MappingTreeBase(Arrays.stream(jsonUtil.readValue(new File(conceptTreeFile), MappingTreeModuleRoot[].class)).toList());

        return Translator.of(MappingContext.of(
                Stream.of(mappings)
                        .collect(Collectors.toMap(Mapping::key, Function.identity(), (a, b) -> a)),
                mappingTreeBase,
                Map.ofEntries(entry("http://fhir.de/CodeSystem/bfarm/icd-10-gm", "icd10"),
                        entry("mii.abide", "abide"),
                        entry("http://fhir.de/CodeSystem/bfarm/ops", "ops"),
                        entry("http://dicom.nema.org/resources/ontology/DCM", "dcm"),
                        entry("https://www.medizininformatik-initiative.de/fhir/core/modul-person/CodeSystem/Vitalstatus", "vitalstatus"),
                        entry("http://loinc.org", "loinc"),
                        entry("https://fhir.bbmri.de/CodeSystem/SampleMaterialType", "sample"),
                        entry("http://fhir.de/CodeSystem/bfarm/atc", "atc"),
                        entry("http://snomed.info/sct", "snomed"),
                        entry("http://terminology.hl7.org/CodeSystem/condition-ver-status", "cvs"),
                        entry("http://hl7.org/fhir/administrative-gender", "gender"),
                        entry("urn:oid:1.2.276.0.76.5.409", "urn409"),
                        entry(
                                "https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/ecrf-parameter-codes",
                                "numecrf"),
                        entry("urn:iso:std:iso:3166", "iso3166"),
                        entry("https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/frailty-score",
                                "frailtyscore"),
                        entry("http://terminology.hl7.org/CodeSystem/consentcategorycodes", "consentcategory"),
                        entry("urn:oid:2.16.840.1.113883.3.1937.777.24.5.3", "consent"),
                        entry("http://hl7.org/fhir/sid/icd-o-3", "icdo3"),
                        entry("fdpg.mii.cds", "fdpgmiicds"),
                        entry("http://fhir.de/CodeSystem/bfarm/alpha-id", "alphaid"),
                        entry("urn:iso:std:iso:11073:10101", "ISO11073"),
                        entry("http://terminology.hl7.org/CodeSystem/icd-o-3", "icdo3"),
                        entry("http://fhir.de/CodeSystem/dkgev/Fachabteilungsschluessel", "fachabteilungsschluessel"),
                        entry("http://terminology.hl7.org/CodeSystem/v3-ActCode", "v3actcode"),
                        entry("http://fhir.de/CodeSystem/dkgev/Fachabteilungsschluessel-erweitert", "fachabteilungsschluesselerweitert"),
                        entry("http://fhir.de/CodeSystem/kontaktart-de", "kontaktart"),
                        entry("http://hl7.org/fhir/sid/icd-10", "sidicd10"),
                        entry("http://fhir.de/CodeSystem/Kontaktebene", "kontaktebene"),
                        entry("http://www.orpha.net", "orphanet"),
                        entry("fdpg.consent.combined", "fdpgcombinedconsent"),
                        entry("http://hl7.org/fhir/consent-provision-type", "provisiontype"),
                        entry("http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation", "v3ObsInterpretation"))));
    }


    @Qualifier("cql")
    @Lazy
    @Bean
    QueryTranslator createCqlQueryTranslator(
            Translator sq2cqlTranslator,
            @Qualifier("translation") ObjectMapper jsonUtil) {
        return new CqlQueryTranslator(sq2cqlTranslator, jsonUtil);
    }

    @Qualifier("fhir")
    @Lazy
    @Bean
    QueryTranslator createFhirQueryBuilder(
            @Qualifier("flare") RestTemplate flareWebClient,
            @Qualifier("translation") ObjectMapper jsonUtil) {
        return new FhirQueryTranslator(flareWebClient, jsonUtil);
    }

    @Qualifier("json")
    @Bean
    QueryTranslator createJsonQueryTranslator(@Qualifier("translation") ObjectMapper jsonUtil) {
        return new JsonQueryTranslator(jsonUtil);
    }

    @Qualifier("flare")
    @Bean
    RestTemplate createFlareWebClient() {
        return new RestTemplateBuilder()
                .rootUri(flareBaseUrl)
                .build();
    }

    @Qualifier("translation")
    @Bean
    ObjectMapper createTranslationObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}
