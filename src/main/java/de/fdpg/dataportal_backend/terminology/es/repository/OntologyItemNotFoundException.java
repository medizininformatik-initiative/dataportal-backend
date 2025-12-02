package de.fdpg.dataportal_backend.terminology.es.repository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class OntologyItemNotFoundException extends RuntimeException {
  public OntologyItemNotFoundException() {}
}
