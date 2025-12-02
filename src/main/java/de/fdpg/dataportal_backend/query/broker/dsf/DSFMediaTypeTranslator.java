package de.fdpg.dataportal_backend.query.broker.dsf;

import de.fdpg.dataportal_backend.query.QueryMediaType;
import de.fdpg.dataportal_backend.query.broker.UnsupportedMediaTypeException;

import java.util.List;

import static de.fdpg.dataportal_backend.query.QueryMediaType.*;

/**
 * Utility for translating backend defined query media types into their corresponding DSF middleware counterpart.
 */
class DSFMediaTypeTranslator {
    /**
     * Given a string representation of a backend defined {@link QueryMediaType}
     * translates it into a media type compatible with DSF middleware instances.
     *
     * @param queryMediaType The {@link QueryMediaType} that shall be translated.
     * @return The translated media type.
     * @throws UnsupportedMediaTypeException If the given media type is not supported.
     */
    public String translate(QueryMediaType queryMediaType) throws UnsupportedMediaTypeException {
        try {
            return switch(queryMediaType) {
                case STRUCTURED_QUERY -> "application/json";
                case CQL -> CQL.getRepresentation();
                case FHIR -> "application/x-fhir-query";
                case JSON -> JSON.getRepresentation();
            };
        } catch (IllegalArgumentException e) {
            throw new UnsupportedMediaTypeException(queryMediaType.getRepresentation(), List.of(
                    STRUCTURED_QUERY.getRepresentation(),
                    CQL.getRepresentation(),
                    FHIR.getRepresentation(),
                    JSON.getRepresentation()));
        }
    }
}
