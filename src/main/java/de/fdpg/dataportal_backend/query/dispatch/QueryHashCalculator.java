package de.fdpg.dataportal_backend.query.dispatch;

import com.google.common.hash.HashFunction;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;

/**
 * Provides functionality for calculating the hash value of a serialized query body.
 */
@RequiredArgsConstructor
public class QueryHashCalculator {

    @NonNull
    private HashFunction hashFn;

    /**
     * Given a serialized query body returns its hash value in hex format.
     *
     * @param queryBody The serialized query whose hash value shall be calculated.
     * @return The calculated hash value in hex format.
     */
    public String calculateSerializedQueryBodyHash(@NotNull String queryBody) {
        if (queryBody == null) {
            throw new IllegalArgumentException("query body must not be null");
        }

        return hashFn.hashString(queryBody, StandardCharsets.UTF_8).toString();
    }
}
