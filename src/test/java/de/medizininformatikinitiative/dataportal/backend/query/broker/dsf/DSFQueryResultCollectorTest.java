package de.medizininformatikinitiative.dataportal.backend.query.broker.dsf;

import de.medizininformatikinitiative.dataportal.backend.query.broker.QueryNotFoundException;
import de.medizininformatikinitiative.dataportal.backend.query.broker.SiteNotFoundException;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DSFQueryResultCollectorTest {

  @Mock
  QueryResultStore store;

  @InjectMocks
  DSFQueryResultCollector collector;

  @Test
  public void testGetResultFeasibilityButQueryIsNotFound() throws QueryNotFoundException, SiteNotFoundException {
    when(store.getMeasureCount("foo", "bar")).thenThrow(QueryNotFoundException.class);
    assertThrows(QueryNotFoundException.class, () -> collector.getResultFeasibility("foo", "bar"));
  }

  @Test
  public void testGetResultFeasibilityButClientIsUnknown() throws QueryNotFoundException, SiteNotFoundException {
    when(store.getMeasureCount("foo", "bar")).thenThrow(SiteNotFoundException.class);
    assertThrows(SiteNotFoundException.class, () -> collector.getResultFeasibility("foo", "bar"));
  }

  @Test
  public void testGetResultFeasibility() throws QueryNotFoundException, SiteNotFoundException {
    when(store.getMeasureCount("foo", "bar")).thenReturn(1);
    int actualResultFeasibility = collector.getResultFeasibility("foo", "bar");

    assertEquals(1, actualResultFeasibility);
  }

  @Test
  public void testGetResultSiteIdsButQueryIsNotFound() throws QueryNotFoundException {
    when(store.getSiteIdsWithResult("foo")).thenThrow(QueryNotFoundException.class);
    assertThrows(QueryNotFoundException.class, () -> collector.getResultSiteIds("foo"));
  }

  @Test
  public void testGetResultSiteIds() throws QueryNotFoundException {
    List<String> expectedSiteIds = Lists.newArrayList("bar", "baz");
    when(store.getSiteIdsWithResult("foo")).thenReturn(expectedSiteIds);

    List<String> actualSiteIds = collector.getResultSiteIds("foo");

    assertEquals(expectedSiteIds, actualSiteIds);
  }

  @Test
  public void testRemoveResultsButQueryIsNotFound() throws QueryNotFoundException {
    Mockito.doThrow(QueryNotFoundException.class).when(store).removeResult("foo");
    assertThrows(QueryNotFoundException.class, () -> collector.removeResults("foo"));
  }
}
