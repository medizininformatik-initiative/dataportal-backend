package de.medizininformatikinitiative.dataportal.backend.terminology.es.repository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class FeatureNotFoundException extends RuntimeException {
  public FeatureNotFoundException() {}
}
