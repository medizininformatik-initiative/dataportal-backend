package de.fdpg.dataportal_backend.query.collect;

import de.fdpg.dataportal_backend.query.broker.BrokerClient;
import lombok.Builder;

/**
 * Defines a status update on a broker specific query.
 * <p>
 * Comprises information about the broker that the update originates from, the associated broker specific query ID, the
 * associated broker specific site ID as well as the query status itself.
 */
@Builder
public record QueryStatusUpdate(BrokerClient source, String brokerQueryId, String brokerSiteId,
                                QueryStatus status) {
}
