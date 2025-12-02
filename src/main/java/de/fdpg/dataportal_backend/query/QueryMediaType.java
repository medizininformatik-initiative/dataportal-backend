package de.fdpg.dataportal_backend.query;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines different kinds of media types regarding different query types supported by this application.
 */
@Getter
@RequiredArgsConstructor
public enum QueryMediaType {
    /**
     * Represents the media type of a structured query.
     */
    STRUCTURED_QUERY("application/sq+json"),
    /**
     * Represents the media type of a CQL query.
     */
    CQL("text/cql"),
    /**
     * Represents the media type of a FHIR search query.
     */
    FHIR("text/fhir-codex"),

    JSON("application/json");

    private final String representation;

    /**
     * Gets a media type entry from its serialized string representation.
     *
     * @param representation The string representation.
     * @return The corresponding media type entry if there is any.
     *
     * @throws IllegalArgumentException If the string representation is not known.
     */
    public static QueryMediaType fromRepresentation(String representation) {
        for (QueryMediaType mediaType : QueryMediaType.values()) {
            if (mediaType.getRepresentation().equals(representation)) {
                return mediaType;
            }
        }
        throw new IllegalArgumentException("No query media type found for the representation '"
                + representation + "'");
    }
}
