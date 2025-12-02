package de.fdpg.dataportal_backend.query.broker.dsf;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Wrapper for different DSF query library information separated by different mime types.
 */
@Getter
final class DSFQueryData {
    private final Map<String, String> contentByType;

    public DSFQueryData() {
        contentByType = new HashMap<>();
    }

    public void addQueryContent(String mediaType, String content) {
        Objects.requireNonNull(mediaType);
        Objects.requireNonNull(content);
        contentByType.put(mediaType, content);
    }

}
