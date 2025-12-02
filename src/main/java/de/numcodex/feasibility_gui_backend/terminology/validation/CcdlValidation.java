package de.numcodex.feasibility_gui_backend.terminology.validation;

import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.MutableCriterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.MutableCcdl;
import de.numcodex.feasibility_gui_backend.query.api.Ccdl;
import de.numcodex.feasibility_gui_backend.query.api.TimeRestriction;
import de.numcodex.feasibility_gui_backend.query.api.status.ValidationIssue;
import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;

import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CcdlValidation {

  private final static String IGNORED_CONSENT_SYSTEM = "fdpg.consent.combined";

  private final TerminologyService terminologyService;

  @Autowired
  public CcdlValidation(TerminologyService terminologyService) {
    this.terminologyService = terminologyService;
  }

  /**
   * Check a CCDL for invalid/outdated termcodes in criteria and annotate it with the issues.
   *
   * For now, just check if the term codes still exist in the current ui profiles. Further
   * iterations may contain checking for availability of values and units of the term codes as well.
   *
   * @param ccdl the ccdl to check
   * @param skipValidation if set to true, the issues list will always be empty
   * @return the Ccdl with issue annotation
   */
  public Ccdl annotateCcdl(Ccdl ccdl, boolean skipValidation) {
    var mutableCcdl = MutableCcdl.createMutableCcdl(ccdl);

    for (List<MutableCriterion> inclusionCriteria : mutableCcdl.getInclusionCriteria()) {
      annotateCriteria(inclusionCriteria, skipValidation);
    }

    for (List<MutableCriterion> exclusionCriteria : mutableCcdl.getExclusionCriteria()) {
      annotateCriteria(exclusionCriteria, skipValidation);
    }

    return Ccdl.createImmutableCcdl(mutableCcdl);
  }

  /**
   * Check a ccdl for invalid/outdated termcodes in criteria and annotate it with the issues.
   *
   * For now, just check if the term codes still exist in the current ui profiles. Further
   * iterations may contain checking for availability of values and units of the term codes as well.
   *
   * @param ccdl the ccdl to check
   * @return the ccdl with issue annotation
   */
  public boolean isValid(Ccdl ccdl) {
    if (ccdl == null) {
      return false;
    }
    if (ccdl.inclusionCriteria() != null) {
      for (List<Criterion> inclusionCriteria : ccdl.inclusionCriteria()) {
        if (containsInvalidCriteria(inclusionCriteria)) {
          return false;
        }
      }
    }

    if (ccdl.exclusionCriteria() != null) {
      for (List<Criterion> exclusionCriteria : ccdl.exclusionCriteria()) {
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
