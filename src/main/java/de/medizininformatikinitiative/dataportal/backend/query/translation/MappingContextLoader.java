package de.medizininformatikinitiative.dataportal.backend.query.translation;

import de.medizininformatikinitiative.cctb.model.AttributeMapping;
import de.medizininformatikinitiative.cctb.model.Mapping;
import de.medizininformatikinitiative.cctb.model.MappingContext;
import de.medizininformatikinitiative.cctb.model.MappingTreeBase;
import de.medizininformatikinitiative.cctb.model.MappingTreeModuleEntry;
import de.medizininformatikinitiative.cctb.model.MappingTreeModuleRoot;
import de.medizininformatikinitiative.cctb.model.common.TermCode;
import de.medizininformatikinitiative.cctb.model.structured_query.ContextualTermCode;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Loads the CCDL to CQL mappings and the mapping tree into a {@link MappingContext} with a small heap footprint.
 * <p>
 * The mapping file holds several hundred thousand mappings, but they are built from a few dozen distinct templates:
 * apart from their key, most mappings repeat the same resource type, paths, attribute mappings and term codes. A plain
 * Jackson deserialization creates separate copies of all of these per mapping, which makes up most of the heap the
 * backend needs at startup (see issue #1245).
 * <p>
 * This loader reads the mappings one at a time and rebuilds each one right away, so that values which are the same -
 * strings, term codes, paths, attribute mappings, lists - are shared instead of duplicated. The resulting
 * {@link MappingContext} holds the same values as one built from a plain deserialization.
 * <p>
 * Values are only shared when they are fully identical. This matters for {@link TermCode}, whose {@code equals} ignores
 * the display, and for every value containing term codes: those are shared by all of their fields, including displays.
 * Shared values are immutable, so no mapping can change a value it shares with others.
 */
@Slf4j
final class MappingContextLoader {

  /**
   * The values seen so far, keyed by themselves or by a {@link TermCodeKey} or {@link FullValueKey} where
   * {@code equals} alone would treat values with different displays as equal.
   */
  private final Map<Object, Object> pool = new HashMap<>();

  MappingContextLoader() {
  }

  /**
   * Loads the mappings and the mapping tree from the given files.
   *
   * @param jsonUtil          the object mapper used to read both files
   * @param mappingsFile      the CQL mappings file, a JSON array of {@link Mapping}s
   * @param conceptTreeFile   the mapping tree file, a JSON array of {@link MappingTreeModuleRoot}s
   * @param codeSystemAliases the code system aliases to use in generated CQL, by code system URL
   * @return the mapping context for the translator
   */
  static MappingContext load(ObjectMapper jsonUtil, File mappingsFile, File conceptTreeFile,
                             Map<String, String> codeSystemAliases) {
    var loader = new MappingContextLoader();
    var mappings = loader.loadMappings(jsonUtil, mappingsFile);
    var mappingTree = loader.loadMappingTree(jsonUtil, conceptTreeFile);
    log.info("Loaded {} CQL mappings from {} and {} mapping tree modules from {}", mappings.size(), mappingsFile,
        mappingTree.moduleRoots().size(), conceptTreeFile);
    return MappingContext.of(mappings, mappingTree, codeSystemAliases);
  }

  /**
   * Reads and rebuilds the mappings one at a time, so the plain deserialized mappings never have to be held all at
   * once. If a key occurs more than once, the first mapping wins.
   */
  Map<ContextualTermCode, Mapping> loadMappings(ObjectMapper jsonUtil, File mappingsFile) {
    var mappings = new HashMap<ContextualTermCode, Mapping>();
    try (var it = jsonUtil.readerFor(Mapping.class).<Mapping>readValues(mappingsFile)) {
      while (it.hasNext()) {
        var mapping = deduplicate(it.next());
        mappings.putIfAbsent(mapping.key(), mapping);
      }
    }
    return mappings;
  }

  MappingTreeBase loadMappingTree(ObjectMapper jsonUtil, File conceptTreeFile) {
    var moduleRoots = new ArrayList<MappingTreeModuleRoot>();
    for (var root : jsonUtil.readValue(conceptTreeFile, MappingTreeModuleRoot[].class)) {
      moduleRoots.add(deduplicate(root));
    }
    return new MappingTreeBase(List.copyOf(moduleRoots));
  }

  private Mapping deduplicate(Mapping mapping) {
    return new Mapping(
        ContextualTermCode.of(deduplicate(mapping.key().context()), deduplicate(mapping.key().termCode())),
        share(mapping.resourceType()),
        // path mappings and time restriction mappings contain no term codes and copy their lists
        share(mapping.valueMapping().orElse(null)),
        shareFullValue(mapping.fixedCriteria()),
        deduplicateAttributeMappings(mapping.attributeMappings()),
        share(mapping.timeRestrictionMapping().orElse(null)),
        deduplicate(mapping.primaryCode()),
        share(mapping.termCodeMapping().orElse(null)));
  }

  private Map<TermCode, AttributeMapping> deduplicateAttributeMappings(
      Map<TermCode, AttributeMapping> attributeMappings) {
    if (attributeMappings.isEmpty()) {
      return Map.of();
    }
    // Map.copyOf would change the iteration order, so keep the map and only make it unmodifiable
    return shareFullValue(Collections.unmodifiableMap(attributeMappings));
  }

  private MappingTreeModuleRoot deduplicate(MappingTreeModuleRoot root) {
    var entries = new HashMap<String, MappingTreeModuleEntry>(root.entries().size() * 4 / 3 + 1);
    root.entries().forEach((key, entry) -> entries.put(share(key), deduplicate(entry)));
    return new MappingTreeModuleRoot(deduplicate(root.context()), share(root.system()), entries);
  }

  private MappingTreeModuleEntry deduplicate(MappingTreeModuleEntry entry) {
    var children = entry.children().isEmpty()
        ? List.<String>of()
        : share(entry.children().stream().map(this::share).toList());
    return new MappingTreeModuleEntry(share(entry.key()), children);
  }

  private TermCode deduplicate(TermCode termCode) {
    if (termCode == null) {
      return null;
    }
    var system = share(termCode.system());
    var code = share(termCode.code());
    var display = share(termCode.display());
    return shareBy(new TermCodeKey(system, code, display), () -> TermCode.of(system, code, display));
  }

  /**
   * Shares a value whose {@code equals} is complete, i.e. one that contains no {@link TermCode}s.
   */
  @SuppressWarnings("unchecked")
  private <T> T share(T value) {
    return value == null ? null : (T) pool.computeIfAbsent(value, v -> v);
  }

  /**
   * Shares an immutable value that may contain {@link TermCode}s, only with values that are equal in every field.
   * The value types are records, whose {@code toString} covers all fields, displays included.
   */
  private <T> T shareFullValue(T value) {
    return value == null ? null : shareBy(new FullValueKey(value, value.toString()), () -> value);
  }

  @SuppressWarnings("unchecked")
  private <T> T shareBy(Object key, Supplier<T> value) {
    return (T) pool.computeIfAbsent(key, k -> value.get());
  }

  private record TermCodeKey(String system, String code, String display) {
  }

  private record FullValueKey(Object value, String fullRepresentation) {
  }
}
