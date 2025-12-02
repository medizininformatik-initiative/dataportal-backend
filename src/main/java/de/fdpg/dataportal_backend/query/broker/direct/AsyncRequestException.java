package de.fdpg.dataportal_backend.query.broker.direct;

import java.io.IOException;

public class AsyncRequestException extends IOException {

  private static final long serialVersionUID = 1L;

  public AsyncRequestException(String message) {
    super(message);
  }

  public AsyncRequestException(String message, Throwable e) {
    super(message, e);
  }

}
