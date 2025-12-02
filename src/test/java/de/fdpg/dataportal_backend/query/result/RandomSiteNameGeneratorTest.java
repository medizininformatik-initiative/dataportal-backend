package de.fdpg.dataportal_backend.query.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RandomSiteNameGeneratorTest {

  @Test
  void checkForCorrectPattern() {
    assertThat(RandomSiteNameGenerator.generateRandomSiteName()).matches("[0-9a-f]{10}");
  }
}
