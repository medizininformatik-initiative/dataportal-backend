package de.fdpg.dataportal_backend.query.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fdpg.dataportal_backend.query.api.StructuredQuery;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A query translator for translating a {@link StructuredQuery} into its JSON representation.
 * Thus, it is less of a translator than a serializer.
 */
@RequiredArgsConstructor
class JsonQueryTranslator implements QueryTranslator {

    @NonNull
    private ObjectMapper jsonUtil;

    @Override
    public String translate(StructuredQuery query) throws QueryTranslationException {
        try {
            return jsonUtil.writeValueAsString(query);
        } catch (JsonProcessingException e) {
            throw new QueryTranslationException("cannot encode structured query as JSON", e);
        }
    }
}
