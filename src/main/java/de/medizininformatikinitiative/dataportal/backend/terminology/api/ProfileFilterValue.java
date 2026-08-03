package de.medizininformatikinitiative.dataportal.backend.terminology.api;

import de.medizininformatikinitiative.dataportal.backend.common.api.DisplayEntry;
import lombok.Builder;

@Builder
public record ProfileFilterValue(
    DisplayEntry display,
    long count
) {
}
