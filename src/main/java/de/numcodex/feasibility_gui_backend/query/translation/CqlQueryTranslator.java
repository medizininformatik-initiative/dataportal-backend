package de.numcodex.feasibility_gui_backend.query.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.Ccdl;
import de.numcodex.sq2cql.Translator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A translator for translating a {@link Ccdl} into its CQL representation.
 */
@RequiredArgsConstructor
class CqlQueryTranslator implements QueryTranslator {

    @NonNull
    private final Translator translator;

    @NonNull
    private final ObjectMapper jsonUtil;

    @Override
    public String translate(Ccdl query) throws QueryTranslationException {
        de.numcodex.sq2cql.model.structured_query.StructuredQuery ccdl;
        try {
            ccdl = jsonUtil.readValue(jsonUtil.writeValueAsString(query),
                    de.numcodex.sq2cql.model.structured_query.StructuredQuery.class);
        } catch (JsonProcessingException e) {
            throw new QueryTranslationException("cannot encode/decode CCDL as JSON", e);
        }

        try {
            return translator.toCql(ccdl).print();
        } catch (Exception e) {
            throw new QueryTranslationException("cannot translate CCDL to CQL format", e);
        }
    }
}
