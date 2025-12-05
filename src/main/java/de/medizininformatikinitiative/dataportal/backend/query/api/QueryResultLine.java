package de.medizininformatikinitiative.dataportal.backend.query.api;

import lombok.Builder;

@Builder
public record QueryResultLine(
    String siteName,
    long numberOfPatients
) {

}
