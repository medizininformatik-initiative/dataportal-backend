package de.numcodex.feasibility_gui_backend.query.translation;

import de.numcodex.feasibility_gui_backend.query.QueryMediaType;
import de.numcodex.feasibility_gui_backend.query.api.Ccdl;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides functions for translating {@link Ccdl} into different formats.
 */
@Slf4j
@RequiredArgsConstructor
public class QueryTranslationComponent {

    @NonNull
    private final Map<QueryMediaType, QueryTranslator> translators;

    /**
     * Translates a {@link Ccdl} into different formats using the configured translators.
     *
     * @param query The query that shall be translated.
     * @return The query translated into different formats mapped to their corresponding media type.
     * @throws QueryTranslationException If any translation fails.
     */
    public Map<QueryMediaType, String> translate(Ccdl query) throws QueryTranslationException {
        var translationResults = new HashMap<QueryMediaType, String>();
        for (Entry<QueryMediaType, QueryTranslator> translatorMapping : translators.entrySet()) {
            var translation = translatorMapping.getValue().translate(query);
            log.debug(translation);
            translationResults.put(translatorMapping.getKey(), translation);
        }

        return translationResults;
    }
}
