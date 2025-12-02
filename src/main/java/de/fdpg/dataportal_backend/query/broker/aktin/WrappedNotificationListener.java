package de.fdpg.dataportal_backend.query.broker.aktin;

import de.fdpg.dataportal_backend.query.collect.QueryStatus;
import de.fdpg.dataportal_backend.query.collect.QueryStatusListener;
import de.fdpg.dataportal_backend.query.collect.QueryStatusUpdate;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.aktin.broker.client2.AdminNotificationListener;
import org.aktin.broker.xml.RequestStatus;

import java.util.logging.Level;

/**
 * Wraps the CODEX notification listener with AKTIN admin notification listener
 * and translates status update notifications.
 * <p>
 * Any unexpected status states are treated as failed.
 *
 * @author R.W.Majeed
 *
 */
@AllArgsConstructor
@Log
public class WrappedNotificationListener implements AdminNotificationListener {
  final private AktinBrokerClient client;
  final private QueryStatusListener statusListener;

  @Override
  public void onRequestCreated(int requestId) {
    // NOP
  }

  @Override
  public void onRequestPublished(int requestId) {
    // NOP
  }

  @Override
  public void onRequestClosed(int requestId) {
    // NOP
  }

  @Override
  public void onRequestStatusUpdate(int requestId, int nodeId, String status) {
    QueryStatus queryStatus;
    RequestStatus rs;
    try {
      rs = RequestStatus.valueOf(status);
    } catch (IllegalArgumentException e) {
      return; // unsupported/unrecognized status
    }

    queryStatus = switch (rs) {
      case completed -> QueryStatus.COMPLETED;
      case processing -> QueryStatus.EXECUTING;
      case queued -> QueryStatus.QUEUED;
      case retrieved -> QueryStatus.RETRIEVED;
      default -> QueryStatus.FAILED;
    };
    log.log(Level.INFO, "Request status updated {0} {1} {2}", new Object[]{requestId, nodeId, status});
    var statusUpdate = QueryStatusUpdate.builder()
        .source(client)
        .brokerQueryId(client.wrapQueryId(requestId))
        .brokerSiteId(client.wrapSiteId(nodeId))
        .status(queryStatus)
        .build();

    var associatedBackendQueryId = client.getBackendQueryId(client.wrapQueryId(requestId));
    statusListener.onClientUpdate(associatedBackendQueryId, statusUpdate);
  }

  @Override
  public void onRequestResultUpdate(int requestId, int nodeId, String mediaType) {
    // NOP
  }

  @Override
  public void onResourceUpdate(int nodeId, String resourceId) {
    // NOP
  }

  @Override
  public void onWebsocketClosed(int statusCode) {
    // NOP
  }

}
