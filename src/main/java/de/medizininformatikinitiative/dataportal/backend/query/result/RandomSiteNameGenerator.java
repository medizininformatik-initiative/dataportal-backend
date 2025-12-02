package de.medizininformatikinitiative.dataportal.backend.query.result;

import java.util.UUID;

/**
 * Generates a random string with a length of 10 by using the last 10 characters of a UUID.
 * <p>
 * UUIDs have the format 8-4-4-4-12. For historical reasons, we currently need 10 characters, so we just use the
 * last 10, meaning the substring begin index is 8+4+4+4+2 + the 4 hyphens = 26
 */
public interface RandomSiteNameGenerator {

  int BEGIN_INDEX = 26;

  static String generateRandomSiteName() {
    return UUID.randomUUID().toString().substring(BEGIN_INDEX);
  }
}
