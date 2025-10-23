package de.medizininformatikinitiative.dataportal.backend.query.translation;

import de.medizininformatikinitiative.dataportal.backend.query.api.Ccdl;

/**
 * Describes an entity that is capable of translating a {@link Ccdl}
 * into another format and returning its string representation.
 */
public interface QueryTranslator {

  /**
   * Translates a given {@link Ccdl} into another format.
   *
   * @param query The ccdl that gets translated.
   * @return A string representation of the translated ccdl in the targeted format.
   * @throws QueryTranslationException If the translation fails.
   */
  String translate(Ccdl query) throws QueryTranslationException
  ;
}
