package de.medizininformatikinitiative.dataportal.backend.query.dispatch;

import com.google.common.hash.Hashing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static java.util.regex.Pattern.matches;
import static org.junit.jupiter.api.Assertions.*;

@Tag("query")
@Tag("dispatch")
public class QueryHashCalculatorTest {

  public static QueryHashCalculator queryHashCalculator;

  @BeforeAll
  public static void setUp() throws NoSuchAlgorithmException {
    queryHashCalculator = new QueryHashCalculator(Hashing.sha256());
  }

  @Test
  public void testCalculateSerializedQueryBodyHash_EmptyStringIsAccepted() {
    assertDoesNotThrow(() -> queryHashCalculator.calculateSerializedQueryBodyHash(""));
  }

  @Test
  public void testCalculateSerializedQueryBodyHash_NullYieldsError() {
    assertThrows(IllegalArgumentException.class, () -> queryHashCalculator.calculateSerializedQueryBodyHash(null));
  }

  @Test
  public void testCalculateSerializedQueryBodyHash_ReturnsHashInHexFormat() {
    var hashValue = assertDoesNotThrow(() -> queryHashCalculator.calculateSerializedQueryBodyHash("foo"));

    System.out.println(hashValue);
    assertTrue(matches("^[a-f0-9]*$", hashValue));
  }
}
