package de.medizininformatikinitiative.dataportal.backend.query.result;

import de.medizininformatikinitiative.dataportal.backend.query.persistence.QueryDispatchRepository;
import org.aktin.broker.client2.BrokerAdmin2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.extra.PeriodDuration;

import java.net.URI;
import java.time.Duration;

import static de.medizininformatikinitiative.dataportal.backend.query.persistence.ResultType.ERROR;
import static de.medizininformatikinitiative.dataportal.backend.query.persistence.ResultType.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("query")
@Tag("result")
@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

  public static final String SITE_NAME = "site-name-142848";
  public static final String SITE_NAME_1 = "site-name-143720";
  public static final String SITE_NAME_2 = "site-name-143723";
  public static final long QUERY_ID = 0L;
  public static final long QUERY_ID_1 = 1L;
  public static final long QUERY_ID_2 = 2L;

  private final PeriodDuration expiryTime = PeriodDuration.of(Duration.ofSeconds(1));

  private ResultService resultService;

  @Mock
  private QueryDispatchRepository queryDispatchRepository;

  @BeforeEach
  void setUp() {
    resultService = new ResultService(expiryTime, new BrokerAdmin2(URI.create("test")), queryDispatchRepository);
  }

  @Test
  void findSuccessfulByQuery_withSuccessResultLine() {
    var resultLine = ResultLine.builder()
        .siteName(SITE_NAME)
        .type(SUCCESS)
        .result(0L)
        .build();
    resultService.addResultLine(QUERY_ID, resultLine);

    var resultLines = resultService.findSuccessfulByQuery(QUERY_ID);

    assertThat(resultLines).singleElement().isEqualTo(resultLine);
  }

  @Test
  void findSuccessfulByQuery_withErrorResultLine() {
    resultService.addResultLine(QUERY_ID,
        ResultLine.builder()
            .siteName(SITE_NAME)
            .type(ERROR)
            .result(0L)
            .build()
    );

    var resultLines = resultService.findSuccessfulByQuery(QUERY_ID);

    assertThat(resultLines).isEmpty();
  }

  @Test
  void testResultExpiry() throws Exception {
    resultService.addResultLine(QUERY_ID,
        ResultLine.builder()
            .siteName(SITE_NAME)
            .type(SUCCESS)
            .result(0L)
            .build()
    );
    Thread.sleep(expiryTime.getDuration().plusMillis(250).toMillis());

    var resultLines = resultService.findSuccessfulByQuery(QUERY_ID);

    assertThat(resultLines).isEmpty();
  }

  @Test
  void testKeepsFirstResultPerSite() {
    ResultLine resultLine = ResultLine.builder()
        .siteName(SITE_NAME)
        .type(SUCCESS)
        .result(0L)
        .build();
    resultService.addResultLine(
        QUERY_ID,
        resultLine);
    resultService.addResultLine(
        QUERY_ID,
        ResultLine.builder()
            .siteName(SITE_NAME)
            .type(ERROR)
            .result(0L)
            .build());

    var resultLines = resultService.findSuccessfulByQuery(QUERY_ID);

    assertThat(resultLines).singleElement().isEqualTo(resultLine);
  }

  @Test
  void testStoresResultsFromMultipleSitesPerQuery() {
    ResultLine resultLine1 = ResultLine.builder()
        .siteName(SITE_NAME_1)
        .type(SUCCESS)
        .result(0L)
        .build();
    resultService.addResultLine(QUERY_ID, resultLine1);
    ResultLine resultLine2 = ResultLine.builder()
        .siteName(SITE_NAME_2)
        .type(SUCCESS)
        .result(0L)
        .build();
    resultService.addResultLine(QUERY_ID, resultLine2);

    var resultLines = resultService.findSuccessfulByQuery(QUERY_ID);

    assertThat(resultLines).contains(resultLine1, resultLine2);
  }

  @Test
  void testStoresResultsFromMultipleQueries() {
    ResultLine resultLine = ResultLine.builder()
        .siteName(SITE_NAME)
        .type(SUCCESS)
        .result(0L)
        .build();
    resultService.addResultLine(QUERY_ID_1, resultLine);
    resultService.addResultLine(QUERY_ID_2, resultLine);

    var resultLines1 = resultService.findSuccessfulByQuery(QUERY_ID_1);
    var resultLines2 = resultService.findSuccessfulByQuery(QUERY_ID_1);

    assertThat(resultLines1).singleElement().isEqualTo(resultLine);
    assertThat(resultLines2).singleElement().isEqualTo(resultLine);
  }
}
