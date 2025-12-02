package de.medizininformatikinitiative.dataportal.backend.terminology.validation;

import de.medizininformatikinitiative.dataportal.backend.common.api.Criterion;
import de.medizininformatikinitiative.dataportal.backend.common.api.MutableCriterion;
import de.medizininformatikinitiative.dataportal.backend.common.api.TermCode;
import de.medizininformatikinitiative.dataportal.backend.query.api.MutableStructuredQuery;
import de.medizininformatikinitiative.dataportal.backend.query.api.StructuredQuery;
import de.medizininformatikinitiative.dataportal.backend.query.api.TimeRestriction;
import de.medizininformatikinitiative.dataportal.backend.query.api.status.ValidationIssue;
import de.medizininformatikinitiative.dataportal.backend.terminology.TerminologyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class StructuredQueryValidation {

  private final static String IGNORED_CONSENT_SYSTEM = "fdpg.consent.combined";

  private final TerminologyService terminologyService;

  @Autowired
  public StructuredQueryValidation(TerminologyService terminologyService) {
    this.terminologyService = terminologyService;
  }

  /**
   * Check a structured query for invalid/outdated termcodes in criteria and annotate it with the issues.
   * <p>
   * For now, just check if the term codes still exist in the current ui profiles. Further
   * iterations may contain checking for availability of values and units of the term codes as well.
   *
   * @param structuredQuery the structured query to check
   * @param skipValidation  if set to true, the issues list will always be empty
   * @return the structuredQuery with issue annotation
   */
  public StructuredQuery annotateStructuredQuery(StructuredQuery structuredQuery, boolean skipValidation) {
    var mutableStructuredQuery = MutableStructuredQuery.createMutableStructuredQuery(structuredQuery);

    for (List<MutableCriterion> inclusionCriteria : mutableStructuredQuery.getInclusionCriteria()) {
      annotateCriteria(inclusionCriteria, skipValidation);
    }

    for (List<MutableCriterion> exclusionCriteria : mutableStructuredQuery.getExclusionCriteria()) {
      annotateCriteria(exclusionCriteria, skipValidation);
    }

    return StructuredQuery.createImmutableStructuredQuery(mutableStructuredQuery);
  }

  /**
   * Check a structured query for invalid/outdated termcodes in criteria and annotate it with the issues.
   * <p>
   * For now, just check if the term codes still exist in the current ui profiles. Further
   * iterations may contain checking for availability of values and units of the term codes as well.
   *
   * @param structuredQuery the structured query to check
   * @return the structuredQuery with issue annotation
   */
  public boolean isValid(StructuredQuery structuredQuery) {
    if (structuredQuery == null) {
      return false;
    }
    if (structuredQuery.inclusionCriteria() != null) {
      for (List<Criterion> inclusionCriteria : structuredQuery.inclusionCriteria()) {
        if (containsInvalidCriteria(inclusionCriteria)) {
          return false;
        }
      }
    }

    if (structuredQuery.exclusionCriteria() != null) {
      for (List<Criterion> exclusionCriteria : structuredQuery.exclusionCriteria()) {
        if (containsInvalidCriteria(exclusionCriteria)) {
          return false;
        }
      }
    }

    return true;
  }

  private void annotateCriteria(List<MutableCriterion> criteria, boolean skipValidation) {
    for (MutableCriterion criterion : criteria) {
      if (skipValidation) {
        criterion.setValidationIssues(List.of());
        continue;
      }
      if (criterion.getContext() == null) {
        criterion.setValidationIssues(List.of(ValidationIssue.TERMCODE_CONTEXT_COMBINATION_INVALID));
        continue;
      }
      if (isTimeRestrictionInvalid(criterion.getTimeRestriction())) {
        criterion.setValidationIssues(List.of(ValidationIssue.TIMERESTRICTION_INVALID));
        continue;
      }
      for (TermCode termCode : criterion.getTermCodes()) {
        if (terminologyService.isExistingTermCode(termCode.system(), termCode.code())) {
          log.trace("termcode ok: {} - {}", termCode.system(), termCode.code());
          criterion.setValidationIssues(List.of()); // empty list is expected
        } else {
          log.debug("termcode invalid: {} - {}", termCode.system(), termCode.code());
          criterion.setValidationIssues(List.of(ValidationIssue.TERMCODE_CONTEXT_COMBINATION_INVALID));
        }
      }
    }
  }

  private boolean containsInvalidCriteria(List<Criterion> criteria) {
    for (Criterion criterion : criteria) {
      if (criterion.context() == null) {
        return true;
      }
      if (isTimeRestrictionInvalid(criterion.timeRestriction())) {
        log.debug("TimeRestriction invalid. 'beforeDate' ({}) must be after 'afterDate' ({}) but is not",
            criterion.timeRestriction().beforeDate(), criterion.timeRestriction().afterDate());
        return true;
      }
      for (TermCode termCode : criterion.termCodes()) {
        // There are some hardcoded consent termcodes in the ui that are not known in the backend. They all are in the same
        // "system"...just skip those in validation
        if (termCode.system().equalsIgnoreCase(IGNORED_CONSENT_SYSTEM)) {
          continue;
        }
        if (terminologyService.isExistingTermCode(termCode.system(), termCode.code())) {
          log.trace("termcode ok: {} - {}", termCode.system(), termCode.code());
        } else {
          log.debug("termcode invalid: {} - {} - {}", termCode.system(), termCode.code(), termCode.version());
          return true;
        }
      }
    }
    return false;
  }

  private boolean isTimeRestrictionInvalid(TimeRestriction timeRestriction) {
    // If no timeRestriction is set or only on of both dates is set, it is not invalid
    if (timeRestriction == null || timeRestriction.beforeDate() == null || timeRestriction.afterDate() == null) {
      return false;
    }
    return LocalDate.parse(timeRestriction.beforeDate()).isBefore(LocalDate.parse(timeRestriction.afterDate()));
  }
}
