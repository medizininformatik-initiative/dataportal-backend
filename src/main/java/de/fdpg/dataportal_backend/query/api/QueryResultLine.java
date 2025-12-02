package de.fdpg.dataportal_backend.query.api;

import lombok.Builder;

@Builder
public record QueryResultLine(
    String siteName,
    long numberOfPatients
) {

}
