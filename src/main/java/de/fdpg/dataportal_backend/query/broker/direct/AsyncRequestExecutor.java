package de.fdpg.dataportal_backend.query.broker.direct;

import ca.uhn.fhir.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
public class AsyncRequestExecutor extends HttpRequestExecutor {
  private static final int WAIT_DURATION_MAX_MS = 30000;
  private static final int WAIT_DURATION_MIN_MS = 250;
  private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

  @Override
  public HttpResponse execute(HttpRequest request, HttpClientConnection conn, HttpContext context)
      throws IOException, HttpException {
    request.addHeader("Prefer", "respond-async");
    var startTime = Instant.now();
    var timeout = Duration
        .ofMillis(((RequestConfig) context.getAttribute(HttpClientContext.REQUEST_CONFIG)).getSocketTimeout());

    HttpResponse initialResponse = super.execute(request, conn, context);

    if (initialResponse.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
      Header contentLocation = initialResponse.getFirstHeader("Content-Location");
      if (contentLocation == null) {
        throw new AsyncRequestException("No Content-Location provided for polling");
      }
      return pollUntilReady(URI.create(contentLocation.getValue()), startTime, timeout, request, conn, context,
          initialResponse);
    }

    return initialResponse;
  }

  private HttpResponse pollUntilReady(URI location, Instant startTime, Duration timeout, HttpRequest origRequest,
                                      HttpClientConnection conn, HttpContext origContext, HttpResponse response)
      throws IOException, HttpException {

    var waitDuration = Duration.ZERO;
    var request = Arrays.stream(origRequest.getAllHeaders())
        .filter(h -> !List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.CONTENT_LENGTH).contains(h.getName()))
        .collect(RequestBuilder::get, (builder, header) -> builder.addHeader(header), (b1, b2) -> {
        })
        .setUri(location.getPath())
        .build();
    var context = new BasicHttpContext(origContext);

    do {
      try {
        log.debug("waiting {} before polling '{}'", waitDuration, location);
        Thread.sleep(waitDuration.toMillis());
        var elapsed = Duration.between(startTime, Instant.now());

        if (elapsed.compareTo(timeout) > 0) {
          log.error("Polling status of asynchronous request at {} timed out after {} (timeout limit: {})",
              location, elapsed, timeout);
          return deleteAsyncRequest(request, conn, context);
        }

        response = super.execute(request, conn, context);
      } catch (InterruptedException e) {
        log.error("Polling status of asynchronous request at {} interrupted: {}", location, e.getMessage());
        return deleteAsyncRequest(request, conn, context);
      }
      waitDuration = nextWaitDuration(response, waitDuration, location);
    } while (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED);

    return response;
  }

  private HttpResponse deleteAsyncRequest(HttpRequest request, HttpClientConnection conn, HttpContext context)
      throws AsyncRequestException {
    try {
      var deleteRequest = Arrays.stream(request.getAllHeaders())
          .collect(RequestBuilder::delete, (builder, header) -> builder.addHeader(header), (b1, b2) -> {
          })
          .setUri(request.getRequestLine().getUri())
          .build();
      var response = super.execute(deleteRequest, conn, context);

      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
        log.info("Asynchronous request at {} cancelled", request.getRequestLine().getUri());
        return response;
      } else {
        log.error("Got http status {} cancelling asynchronous request at {}",
            response.getStatusLine().getStatusCode(), request.getRequestLine().getUri());
        return response;
      }
    } catch (IOException | HttpException e) {
      throw new AsyncRequestException(
          "Failed to cancel asynchronous request at %s".formatted(request.getRequestLine().getUri()),
          e);
    }
  }

  private Duration nextWaitDuration(HttpResponse response, Duration previousWaitDuration, URI location) {
    if (response.containsHeader(HttpHeaders.RETRY_AFTER)) {
      var retryHeader = response.getFirstHeader(HttpHeaders.RETRY_AFTER);
      return Optional.ofNullable(retryHeader)
          .map(h -> h.getValue())
          .map(DateUtils::parseDate)
          .map(Date::getTime)
          .map(t -> Duration.ofMillis(Math.max(t - System.currentTimeMillis(), 0)))
          .orElseGet(() -> Optional.ofNullable(retryHeader)
              .map(h -> h.getValue())
              .filter(h -> NUMBER_PATTERN.matcher(h).matches())
              .map(BigDecimal::new)
              .map(BigDecimal::longValue)
              .map(Duration::ofSeconds)
              .orElseGet(() -> {
                log.error("Response from {} contains invalid Retry-After header value: {}",
                    location,
                    retryHeader.getValue());
                return exponentialWaitDuration(previousWaitDuration);
              }));
    } else {
      return exponentialWaitDuration(previousWaitDuration);
    }
  }

  private Duration exponentialWaitDuration(Duration previousWaitDuration) {
    return Duration.ofMillis(Math.min(WAIT_DURATION_MAX_MS,
        Math.max(WAIT_DURATION_MIN_MS, previousWaitDuration.toMillis() * 2)));
  }
}
