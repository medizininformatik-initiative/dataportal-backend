package de.numcodex.feasibility_gui_backend.query.api.status;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record IssueWrapper(
    @NonNull String path,
    @NonNull Object value
) {
}
