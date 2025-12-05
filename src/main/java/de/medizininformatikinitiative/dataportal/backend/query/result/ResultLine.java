package de.medizininformatikinitiative.dataportal.backend.query.result;

import de.medizininformatikinitiative.dataportal.backend.query.broker.BrokerClient;
import de.medizininformatikinitiative.dataportal.backend.query.persistence.ResultType;
import lombok.Builder;

import java.util.Objects;

/**
 * Represents one submitted result from one connected site.
 * <p>
 * A ResultLine is received via one of the connected
 * {@link BrokerClient BrokerClient}
 * implementations. It holds information about the site name (as submitted by the
 * {@code BrokerClient}), the {@link ResultType} (success or error)and the number of patients.
 */
@Builder
public record ResultLine(String siteName, ResultType type, long result) {

  public ResultLine {
    Objects.requireNonNull(siteName);
    Objects.requireNonNull(type);
  }
}
