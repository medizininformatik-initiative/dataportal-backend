package de.medizininformatikinitiative.dataportal.backend.query.translation;

import de.medizininformatikinitiative.cctb.Translator;
import de.medizininformatikinitiative.cctb.model.Mapping;
import de.medizininformatikinitiative.cctb.model.MappingContext;
import de.medizininformatikinitiative.cctb.model.MappingTreeBase;
import de.medizininformatikinitiative.cctb.model.MappingTreeModuleRoot;
import de.medizininformatikinitiative.cctb.model.common.TermCode;
import de.medizininformatikinitiative.cctb.model.structured_query.ContextualTermCode;
import de.medizininformatikinitiative.cctb.model.structured_query.StructuredQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The fixture holds one mapping for each of the distinct mapping templates in the ontology, the tree children of
 * those mappings that have them, a mapping with a duplicate key, and two mappings with term codes that differ from
 * existing ones only in their display.
 */
@Tag("query")
@Tag("translation")
class MappingContextLoaderTest {

  private static final ObjectMapper JSON_UTIL = JsonMapper.builderWithJackson2Defaults().build();

  private static File mappingsFile;
  private static File conceptTreeFile;

  /** Loaded the way the backend did before the loader existed. */
  private static Map<ContextualTermCode, Mapping> plainMappings;
  private static MappingTreeBase plainMappingTree;

  private static Map<ContextualTermCode, Mapping> mappings;
  private static MappingTreeBase mappingTree;

  @BeforeAll
  static void setUp() throws URISyntaxException {
    mappingsFile = resource("dedup/mapping_cql.json");
    conceptTreeFile = resource("dedup/mapping_tree.json");

    plainMappings = Arrays.stream(JSON_UTIL.readValue(mappingsFile, Mapping[].class))
        .collect(Collectors.toMap(Mapping::key, Function.identity(), (a, b) -> a));
    plainMappingTree = new MappingTreeBase(
        Arrays.stream(JSON_UTIL.readValue(conceptTreeFile, MappingTreeModuleRoot[].class)).toList());

    mappings = new MappingContextLoader().loadMappings(JSON_UTIL, mappingsFile);
    mappingTree = new MappingContextLoader().loadMappingTree(JSON_UTIL, conceptTreeFile);
  }

  private static File resource(String name) throws URISyntaxException {
    return new File(Objects.requireNonNull(MappingContextLoaderTest.class.getResource(name)).toURI());
  }

  @Test
  void loadsTheSameMappingsAsAPlainDeserialization() {
    assertThat(plainMappings).hasSizeGreaterThan(100);
    assertThat(mappings.keySet()).isEqualTo(plainMappings.keySet());

    // TermCode.equals ignores the display, so compare the full representations, which include all displays
    plainMappings.forEach((key, expected) ->
        assertThat(fullRepresentation(mappings.get(key))).isEqualTo(fullRepresentation(expected)));
  }

  @Test
  void keepsDisplaysOfTermCodesThatAreOnlyEqualBySystemAndCode() {
    var variant = mappings.values().stream()
        .filter(m -> m.key().context().code().equals("DisplayVariantContext")).findFirst().orElseThrow();
    var original = mappings.values().stream()
        .filter(m -> m != variant && m.key().termCode().equals(variant.key().termCode())).findFirst().orElseThrow();

    assertThat(variant.key().termCode().display()).startsWith("Display variant of");
    assertThat(original.key().termCode().display()).doesNotStartWith("Display variant of");

    var attributeVariant = mappings.values().stream()
        .filter(m -> m.key().termCode().code().startsWith("display-variant-")).findFirst().orElseThrow();
    assertThat(attributeVariant.attributeMappings().keySet()).extracting(TermCode::display)
        .anySatisfy(display -> assertThat(display).startsWith("Display variant of attribute"));
    assertThat(mappings.values()).filteredOn(m -> m != attributeVariant)
        .flatExtracting(m -> m.attributeMappings().keySet())
        .extracting(TermCode::display)
        .noneSatisfy(display -> assertThat(display).startsWith("Display variant of attribute"));
  }

  @Test
  void sharedListsAndMapsAreUnmodifiable() {
    var withAttributes = mappings.values().stream()
        .filter(m -> !m.attributeMappings().isEmpty()).findFirst().orElseThrow();
    var withFixedCriteria = mappings.values().stream()
        .filter(m -> !m.fixedCriteria().isEmpty()).findFirst().orElseThrow();
    var withChildren = mappingTree.moduleRoots().stream().flatMap(root -> root.entries().values().stream())
        .filter(entry -> !entry.children().isEmpty()).findFirst().orElseThrow();

    assertThatThrownBy(() -> withAttributes.attributeMappings().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> withFixedCriteria.fixedCriteria().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> withChildren.children().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void keepsTheFirstMappingOfADuplicateKey() {
    assertThat(mappings.values()).extracting(Mapping::resourceType).doesNotContain("DuplicateMustBeIgnored");
  }

  @Test
  void loadsTheSameMappingTreeAsAPlainDeserialization() {
    assertThat(mappingTree).isEqualTo(plainMappingTree);
    assertThat(mappingTree.moduleRoots()).flatExtracting(root -> root.entries().values())
        .anySatisfy(entry -> assertThat(entry.children()).isNotEmpty());
  }

  @Test
  void sharesEqualValuesBetweenMappings() {
    var contexts = new IdentityHashMap<TermCode, Boolean>();
    var systems = new IdentityHashMap<String, Boolean>();
    var timeRestrictionMappings = new IdentityHashMap<Object, Boolean>();
    mappings.values().forEach(mapping -> {
      contexts.put(mapping.key().context(), true);
      systems.put(mapping.key().termCode().system(), true);
      mapping.timeRestrictionMapping().ifPresent(trm -> timeRestrictionMappings.put(trm, true));
    });

    assertThat(contexts).hasSameSizeAs(mappings.values().stream().map(m -> m.key().context()).distinct().toList());
    assertThat(systems).hasSameSizeAs(
        mappings.values().stream().map(m -> m.key().termCode().system()).distinct().toList());
    assertThat(timeRestrictionMappings).hasSameSizeAs(mappings.values().stream()
        .flatMap(m -> m.timeRestrictionMapping().stream()).distinct().toList());
  }

  @Test
  void translatesEveryMappingToTheSameCqlAsAPlainDeserialization() {
    var aliases = QueryTranslatorSpringConfig.CODE_SYSTEM_ALIASES;
    var plainTranslator = Translator.of(MappingContext.of(plainMappings, plainMappingTree, aliases));
    var translator = Translator.of(MappingContextLoader.load(JSON_UTIL, mappingsFile, conceptTreeFile, aliases));

    var translated = 0;
    for (var key : plainMappings.keySet()) {
      var query = queryFor(key);
      var expected = translate(plainTranslator, query);
      assertThat(translate(translator, query)).as("CQL for %s", key).isEqualTo(expected);
      if (!expected.startsWith("ERROR")) {
        translated++;
      }
    }
    assertThat(translated).as("number of mappings translated without error").isGreaterThan(plainMappings.size() / 2);
  }

  private static String fullRepresentation(Mapping mapping) {
    return String.join("|", mapping.key().toString(), mapping.resourceType(), mapping.valueMapping().toString(),
        mapping.fixedCriteria().toString(), mapping.attributeMappings().toString(),
        mapping.timeRestrictionMapping().toString(), String.valueOf(mapping.primaryCode()),
        mapping.termCodeMapping().toString());
  }

  private static StructuredQuery queryFor(ContextualTermCode key) {
    var criterion = Map.of(
        "context", termCodeJson(key.context()),
        "termCodes", List.of(termCodeJson(key.termCode())));
    return JSON_UTIL.convertValue(Map.of("inclusionCriteria", List.of(List.of(criterion))), StructuredQuery.class);
  }

  private static Map<String, String> termCodeJson(TermCode termCode) {
    return Map.of("system", termCode.system(), "code", termCode.code(), "display", termCode.display());
  }

  private static String translate(Translator translator, StructuredQuery query) {
    try {
      return translator.toCql(query).print();
    } catch (Exception e) {
      return "ERROR " + e.getClass().getName() + ": " + e.getMessage();
    }
  }
}
