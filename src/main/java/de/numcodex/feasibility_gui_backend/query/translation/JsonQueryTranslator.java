package de.numcodex.feasibility_gui_backend.query.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.Ccdl;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A query translator for translating a {@link Ccdl} into its JSON representation.
 * Thus, it is less of a translator than a serializer.
 */
@RequiredArgsConstructor
class JsonQueryTranslator implements QueryTranslator {

    @NonNull
    private ObjectMapper jsonUtil;

    @Override
    public String translate(Ccdl query) throws QueryTranslationException {
        try {
            return jsonUtil.writeValueAsString(query);
        } catch (JsonProcessingException e) {
            throw new QueryTranslationException("cannot encode CCDL as JSON", e);
        }
    }
}
