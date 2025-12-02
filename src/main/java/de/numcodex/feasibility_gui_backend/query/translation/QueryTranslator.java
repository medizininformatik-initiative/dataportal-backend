package de.numcodex.feasibility_gui_backend.query.translation;

import de.numcodex.feasibility_gui_backend.query.api.Ccdl;

/**
 * Describes an entity that is capable of translating a @{link Ccdl}
 * into another format and returning its string representation.
 */
public interface QueryTranslator {

    /**
     * Translates a given {@link Ccdl} into another format.
     *
     * @param query The CCDL that gets translated.
     * @return A string representation of the translated CCDL in the targeted format.
     *
     * @throws QueryTranslationException If the translation fails.
     */
    String translate(Ccdl query) throws QueryTranslationException
    ;
}
