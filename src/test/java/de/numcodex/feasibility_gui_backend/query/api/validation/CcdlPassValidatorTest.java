package de.numcodex.feasibility_gui_backend.query.api.validation;

import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.Ccdl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("query")
@Tag("api")
@Tag("validation")
@ExtendWith(MockitoExtension.class)
public class CcdlPassValidatorTest {

    @Spy
    private CcdlPassValidator validator;

    @Mock
    private ConstraintValidatorContext ctx;

    @Test
    public void testIsValid_validQueryPasses() {
        var termCode = TermCode.builder()
                .code("LL2191-6")
                .system("http://loinc.org")
                .display("Geschlecht")
                .build();
        var inclusionCriterion = Criterion.builder()
                .termCodes(List.of(termCode))
                .build();
        var testQuery = Ccdl.builder()
                .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
                .inclusionCriteria(List.of(List.of(inclusionCriterion)))
                .exclusionCriteria(List.of())
                .display("foo")
                .build();

        var validationResult = assertDoesNotThrow(() -> validator.isValid(testQuery, ctx));
        assertTrue(validationResult);
    }

    @Test
    public void testIsValid_invalidQueryPasses() {
        var testQuery = Ccdl.builder().build();
        var validationResult = assertDoesNotThrow(() -> validator.isValid(testQuery, ctx));
        assertTrue(validationResult);
    }
}
